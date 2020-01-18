package integration;

import fm.bootifulpodcast.rabbitmq.RabbitMqHelper;
import integration.aws.AwsS3Service;
import integration.events.PodcastArchiveUploadedEvent;
import integration.events.PodcastArtifactsUploadedToProcessorEvent;
import integration.utils.FileUtils;
import integration.utils.JsonHelper;
import integration.utils.UnzipUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.AggregatorSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static integration.Headers.*;

/**
 * This is step 1 in the flow.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Log4j2
@Configuration
class Step1UploadPreparationIntegrationConfiguration {

	private final GenericHandler<File> s3UploadHandler;

	private final GenericHandler<Map> rmqProcessorAggregateArtifactsTransformer;

	private final Consumer<AggregatorSpec> aggregator;

	private final PipelineProperties properties;

	private final GenericHandler<Object> uploadProfilePhotoHandler;

	private final Function<File, Collection<Message<File>>> unzipSplitter;

	private final ApplicationEventPublisher publisher;

	Step1UploadPreparationIntegrationConfiguration(AwsS3Service s3,
			JsonHelper jsonService, PipelineProperties properties,
			ApplicationEventPublisher publisher) {
		this.properties = properties;
		this.publisher = publisher;
		var retryTemplate = new RetryTemplate();
		this.unzipSplitter = (file) -> {
			var stagingDirectoryForRequest = FileUtils.ensureDirectoryExists(
					new File(properties.getS3().getStagingDirectory(),
							UUID.randomUUID().toString()));
			var files = UnzipUtils.unzip(file, stagingDirectoryForRequest);
			Assert.isTrue(!file.exists() || file.delete(), "the uploaded file "
					+ file.getAbsolutePath() + " could not be deleted.");
			var manifest = files.stream()
					.filter(fn -> fn.getName().toLowerCase().endsWith("manifest.xml"))
					.collect(Collectors.toList());
			Assert.isTrue(manifest.size() > 0,
					"at least one file must be a manifest.xml file for a package to be considered valid.");
			var manifestFile = manifest.get(0);
			Assert.notNull(manifest, "the manifest must not be null");
			var uploadPackageManifest = PodcastPackageManifest.from(manifestFile);
			this.publisher
					.publishEvent(new PodcastArchiveUploadedEvent(uploadPackageManifest));
			var stream = files.stream().map(f -> {

				var fileName = f.getName();
				var isIntro = fileName.equalsIgnoreCase(
						uploadPackageManifest.getIntroduction().getSrc());
				var isInterview = fileName
						.equalsIgnoreCase(uploadPackageManifest.getInterview().getSrc());
				var isPhoto = fileName
						.equalsIgnoreCase(uploadPackageManifest.getPhoto().getSrc());

				// i can't wait to use this with a smarter case statement
				String assetType = null;
				if (isPhoto) {
					assetType = AssetTypes.TYPE_PHOTO;
				}
				if (isInterview) {
					assetType = AssetTypes.TYPE_INTERVIEW;
				}
				if (isIntro) {
					assetType = AssetTypes.TYPE_INTRODUCTION;
				}

				var builder = MessageBuilder//
						.withPayload(f)//
						.setHeader(CONTENT_TYPE, determineContentTypeFor(f))//
						.setHeader(ARTIFACT_STAGING_DIRECTORY, stagingDirectoryForRequest)//
						.setHeader(PACKAGE_MANIFEST, uploadPackageManifest)
						.setHeader(IS_PHOTO_FILE, isPhoto)
						.setHeader(IS_INTERVIEW_FILE, isInterview)
						.setHeader(IS_INTRODUCTION_FILE, isIntro);

				// todo remove the ASSET_TYPE do we ever use it?
				if (StringUtils.hasText(assetType)) {
					builder.setHeader(ASSET_TYPE, assetType);
				}
				return builder.build();
			});
			return stream.collect(Collectors.toList());
		};

		this.s3UploadHandler = (file, messageHeaders) -> {
			var contentType = messageHeaders.get(CONTENT_TYPE, String.class);
			var manifest = Objects.requireNonNull(
					messageHeaders.get(PACKAGE_MANIFEST, PodcastPackageManifest.class));
			var uid = manifest.getUid();
			Assert.notNull(uid, "the UID must not be null");
			log.info("begin: s3 artifact upload " + file.getAbsolutePath());
			var s3Path = retryTemplate.execute(context -> {
				log.info("trying to upload " + file.getAbsolutePath()
						+ " with content-type " + contentType + " with UID " + uid
						+ ", attempt #" + context.getRetryCount());
				return s3.uploadInputFile(contentType, uid, file);
			});
			log.info("end: s3 artifact upload " + file.getAbsolutePath());
			var assetType = messageHeaders.get(ASSET_TYPE, String.class);
			log.info("the asset type is '" + assetType + "' and the s3 path is '" + s3Path
					+ "'");
			var uriAsString = s3Path.toString();
			this.publisher.publishEvent(new PodcastArtifactsUploadedToProcessorEvent(uid,
					assetType, uriAsString, file));
			Assert.isTrue(!file.exists() || file.delete(),
					"the file " + file.getAbsolutePath() + " has been uploaded so we "
							+ "are purging it from the local file system.");
			return MessageBuilder //
					.withPayload(file) //
					.setHeader(ARTIFACT_STAGING_DIRECTORY,
							messageHeaders.get(ARTIFACT_STAGING_DIRECTORY))
					.setHeader(S3_PATH, uriAsString) //
					.build();
		};

		this.aggregator = spec -> spec.outputProcessor(group -> {
			var request = new HashMap<String, String>();
			group.getMessages().forEach(msg -> {
				var manifest = msg.getHeaders().get(PACKAGE_MANIFEST,
						PodcastPackageManifest.class);
				log.info("aggregating " + PodcastPackageManifest.class.getName()
						+ " with UID " + manifest.getUid());
				establishHeaderIfMatches(request, msg, IS_INTRODUCTION_FILE,
						PROCESSOR_REQUEST_INTRODUCTION);
				establishHeaderIfMatches(request, msg, IS_INTERVIEW_FILE,
						PROCESSOR_REQUEST_INTERVIEW);
				var uid = Objects.requireNonNull(manifest).getUid();
				request.put("uid", uid);
				var stagingDirectory = (File) msg.getHeaders()
						.get(ARTIFACT_STAGING_DIRECTORY);
				Assert.isTrue(
						!Objects.requireNonNull(stagingDirectory).exists()
								|| FileUtils.deleteDirectoryRecursively(stagingDirectory),
						"the staging directory " + stagingDirectory.getAbsolutePath()
								+ " could not be deleted.");
			});
			return request;
		});

		this.uploadProfilePhotoHandler = (o, messageHeaders) -> {
			try {
				log.info("entering the upload profile photo handler");
				var manifest = messageHeaders.get(PACKAGE_MANIFEST,
						PodcastPackageManifest.class);
				var uid = Objects.requireNonNull(manifest).getUid();

				// todo
				// todo download the input file for the photo
				var tmpFile = Files.createTempFile(uid, ".jpg").toFile();
				try (var inputStream = s3
						.downloadInputFile(uid, manifest.getPhoto().getSrc())
						.getObjectContent();
						var outputStream = new FileOutputStream(tmpFile)) {
					FileCopyUtils.copy(inputStream, outputStream);
					Assert.isTrue(tmpFile.exists(), "the profile photo '"
							+ tmpFile.getAbsolutePath() + "' could not be downloaded");
					var uploadOutputFile = s3.uploadOutputFile(MediaType.IMAGE_JPEG_VALUE,
							uid, tmpFile);
					log.info("uploaded the photo '" + tmpFile.getAbsolutePath()
							+ "' to the output bucket. It has the URI '"
							+ uploadOutputFile + "'");
					return o;
				}
			}
			catch (Exception e) {
				ReflectionUtils.rethrowRuntimeException(e);
			}
			return o;
		};

		this.rmqProcessorAggregateArtifactsTransformer = (payload, headers) -> {
			var json = jsonService.toJson(payload);
			var builder = MessageBuilder.withPayload(json);
			Set.of(UID, PROCESSOR_REQUEST_INTERVIEW, PROCESSOR_REQUEST_INTRODUCTION)
					.forEach(header -> builder.setHeader(header, payload.get(header)));
			return builder.build();
		};
	}

	private static String determineContentTypeFor(File file) {
		Assert.notNull(file, "the file must not be null");
		var map = Map.of(//
				"wav", "audio/wav", //
				"mp3", "audio/mp3", //
				"xml", "application/xml", //
				"jpg", "image/jpeg");
		var fn = file.getName().toLowerCase();
		for (var ext : map.keySet()) {
			if (fn.endsWith(ext)) {
				return map.get(ext);
			}
		}
		throw new RuntimeException("Invalid file-type!");
	}

	private static void establishHeaderIfMatches(Map<String, String> request,
			Message<?> msg, String header, String newKey) {
		var isTrue = msg.getHeaders().containsKey(header)
				&& Objects.requireNonNull(msg.getHeaders().get(header, Boolean.class));
		if (isTrue) {
			request.put(newKey, msg.getHeaders().get(S3_PATH, String.class));
		}
	}

	@Bean
	IntegrationFlow uploadPreparationIntegrationFlow(AmqpTemplate template,
			RabbitMqHelper helper) {

		var processorConfig = this.properties.getProcessor();

		var processorOutboundAdapter = Amqp//
				.outboundAdapter(template)//
				.exchangeName(processorConfig.getRequestsExchange()) //
				.routingKey(processorConfig.getRequestsRoutingKey());

		helper.defineDestination(processorConfig.getRequestsExchange(),
				processorConfig.getRequestsQueue(),
				processorConfig.getRequestsRoutingKey());

		helper.defineDestination(processorConfig.getRepliesExchange(),
				processorConfig.getRepliesQueue(),
				processorConfig.getRepliesRoutingKey());

		return IntegrationFlows//
				.from(this.uploadsMessageChannel()) //
				.transform(String.class, File::new)//
				.split(File.class, this.unzipSplitter) //
				.handle(File.class, this.s3UploadHandler) //
				.aggregate(this.aggregator) //
				.handle(this.uploadProfilePhotoHandler)
				.handle(Map.class, this.rmqProcessorAggregateArtifactsTransformer)//
				.handle(processorOutboundAdapter)//
				.get();
	}

	@Bean
	MessageChannel uploadsMessageChannel() {
		return MessageChannels.direct().get();
	}

}
