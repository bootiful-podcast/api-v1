package integration;

import com.fasterxml.jackson.core.type.TypeReference;
import integration.aws.AwsS3Service;
import integration.events.PodcastArchiveUploadedEvent;
import integration.events.PodcastArtifactsUploadedToProcessorEvent;
import integration.events.PodcastProcessedEvent;
import integration.rabbitmq.RabbitHelper;
import integration.utils.FileUtils;
import integration.utils.JsonHelper;
import integration.utils.UnzipUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.AggregatorSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static integration.Headers.*;

@Log4j2
@Configuration
class IntegrationFlowConfiguration {

	private final PipelineProperties properties;

	private final JsonHelper json;

	private final Function<File, Collection<Message<File>>> unzipSplitter;

	private final GenericHandler<File> s3UploadHandler;

	private final Consumer<AggregatorSpec> aggregator;

	private final GenericHandler<Map> rmqProcessorAggregateArtifactsTransformer;

	private final ApplicationEventPublisher publisher;

	IntegrationFlowConfiguration(PipelineProperties properties, JsonHelper jsonService,
			AwsS3Service s3, ApplicationEventPublisher publisher) {
		var retryTemplate = new RetryTemplate();
		this.properties = properties;
		this.publisher = publisher;
		this.json = jsonService;
		this.unzipSplitter = (file) -> {
			var stagingDirectoryForRequest = FileUtils.ensureDirectoryExists(
					new File(properties.getS3().getStagingDirectory(),
							UUID.randomUUID().toString()));

			var files = UnzipUtils.unzip(file, stagingDirectoryForRequest);
			var manifest = files.stream()
					.filter(fn -> fn.getName().toLowerCase().endsWith("manifest.xml"))
					.collect(Collectors.toList());
			Assert.isTrue(manifest.size() > 0,
					"at least one file must be a manifest.xml file for a package to be considered valid.");
			var manifestFile = manifest.get(0);
			Assert.notNull(manifest, "the manifest must not be null");
			var uploadPackageManifest = PodcastPackageManifest.from(manifestFile);
			recordUploadPackageManifest(uploadPackageManifest);
			var stream = files.stream().map(f -> {
				var builder = MessageBuilder//
						.withPayload(f)//
						.setHeader(CONTENT_TYPE, determineContentTypeFor(f))//
						.setHeader(PACKAGE_MANIFEST, uploadPackageManifest);

				uploadPackageManifest.getMedia().forEach(media -> {
					var interview = f.getName().contains(media.getInterview());
					var intro = f.getName().contains(media.getIntroduction());
					var type = interview ? AssetTypes.TYPE_INTERVIEW
							: (intro ? AssetTypes.TYPE_INTRODUCTION : null);
					var mediaMap = Map.of( //
							IS_INTERVIEW_FILE, interview, //
							IS_INTRODUCTION_FILE, intro, //
							//
							ARTIFACT_STAGING_DIRECTORY, stagingDirectoryForRequest //
					);
					if (StringUtils.hasText(type)) {
						builder.setHeader(ASSET_TYPE, type);
					}
					mediaMap.forEach(builder::setHeader);
				});
				return builder.build();
			});
			return stream.collect(Collectors.toList());
		};

		this.s3UploadHandler = (file, messageHeaders) -> {
			var contentType = messageHeaders.get(CONTENT_TYPE, String.class);
			var manifest = messageHeaders.get(PACKAGE_MANIFEST,
					PodcastPackageManifest.class);
			var uid = manifest.getUid();
			Assert.notNull(uid, "the UID must not be null");
			log.info("begin: s3 artifact upload " + file.getAbsolutePath());
			var s3Path = retryTemplate.execute(context -> {

				log.info("trying to upload " + file.getAbsolutePath()
					+ " with content-type " + contentType + " with UID " + uid
					+ ", attempt #" + context.getRetryCount());

				return s3.upload(contentType, uid, file);
			});
			log.info("end: s3 artifact upload " + file.getAbsolutePath());
			var role = messageHeaders.get(ASSET_TYPE, String.class);
			log.info("the asset type is " + role);
			log.info("the s3 path is " + s3Path);
			var uriAsString = s3Path.toString();
			publisher.publishEvent(new PodcastArtifactsUploadedToProcessorEvent(uid, role,
					uriAsString, file));
			return MessageBuilder //
					.withPayload(file) //
					.setHeader(S3_PATH, uriAsString) //
					.build();
		};

		this.aggregator = spec -> spec.outputProcessor(group -> {
			var messages = group.getMessages();
			var request = new HashMap<String, String>();
			messages.forEach(msg -> {
				establishHeaderIfMatches(request, msg, IS_INTRODUCTION_FILE,
						PROCESSOR_REQUEST_INTRODUCTION);
				establishHeaderIfMatches(request, msg, IS_INTERVIEW_FILE,
						PROCESSOR_REQUEST_INTERVIEW);
				var manifest = msg.getHeaders().get(PACKAGE_MANIFEST,
						PodcastPackageManifest.class);
				var uid = Objects.requireNonNull(manifest).getUid();
				request.put("uid", uid);
			});
			return request;
		});

		this.rmqProcessorAggregateArtifactsTransformer = (payload, headers) -> {
			var json = jsonService.toJson(payload);
			var builder = MessageBuilder.withPayload(json);
			Set.of(UID, PROCESSOR_REQUEST_INTERVIEW, PROCESSOR_REQUEST_INTRODUCTION)
					.forEach(header -> builder.setHeader(header, payload.get(header)));
			return builder.build();
		};
	}

	private static void establishHeaderIfMatches(Map<String, String> request,
																																														Message<?> msg, String header, String newKey) {
		if (isTrue(msg.getHeaders(), header)) {
			request.put(newKey, msg.getHeaders().get(S3_PATH, String.class));
		}
	}

	private static String determineContentTypeFor(File file) {
		Assert.notNull(file, "the file must not be null");
		var map = Map.of(//
				"wav", "audio/wav", //
				"mp3", "audio/mp3", //
				"xml", "application/xml" //
		);
		var fn = file.getName().toLowerCase();
		for (var ext : map.keySet()) {
			if (fn.endsWith(ext)) {
				return map.get(ext);
			}
		}
		throw new RuntimeException("Invalid file-type!");
	}

	private static Boolean isTrue(MessageHeaders headers, String header) {
		return headers.get(header, Boolean.class);
	}

	@Bean
	IntegrationFlow audioProcessorReplyPipeline(ConnectionFactory connectionFactory) {
		var repliesQueue = properties.getProcessor().getRepliesQueue();
		var amqpInboundAdapter = Amqp //
				.inboundAdapter(connectionFactory, repliesQueue) //
				.get();
		return IntegrationFlows //
				.from(amqpInboundAdapter) //
				.handle(String.class, (payload, headers) -> {
					var reference = new TypeReference<Map<String, String>>() {
					};
					var resultMap = json.fromJson(payload, reference);
					var outputFileExtension = resultMap.getOrDefault("mp3",
							resultMap.getOrDefault("wav", null));
					var uid = resultMap.get("uid");
					var outputBucketName = resultMap.get("output-bucket-name");
					this.recordProcessedFilesToDatabase(uid, outputBucketName,
							outputFileExtension);
					return null;
				}).get();
	}

	private void recordUploadPackageManifest(PodcastPackageManifest packageManifest) {
		this.publisher.publishEvent(new PodcastArchiveUploadedEvent(packageManifest));
	}

	private void recordProcessedFilesToDatabase(String uid, String outputBucketName,
			String fileName) {
		var event = new PodcastProcessedEvent(uid, outputBucketName, fileName);
		this.publisher.publishEvent(event);
	}

	@Bean
	IntegrationFlow audioProcessorPreparationPipeline(RabbitHelper helper) {

		var processorConfig = properties.getProcessor();

		helper.defineDestination(processorConfig.getRequestsExchange(),
			processorConfig.getRequestsQueue(),
			processorConfig.getRequestsRoutingKey());

		helper.defineDestination(processorConfig.getRepliesExchange(),
			processorConfig.getRepliesQueue(),
			processorConfig.getRepliesRoutingKey());

		return IntegrationFlows//
			.from(apiToPipelineChannel()) //
			.split(File.class, this.unzipSplitter) //
			.channel(concurrentQueue()).get();
	}

	@Bean
	IntegrationFlow concurrentIntegrationFlow(AmqpTemplate template) {

		var processorConfig = properties.getProcessor();
		var processorOutboundAdapter = Amqp //
			.outboundAdapter(template)//
			.exchangeName(processorConfig.getRequestsExchange()) //
			.routingKey(processorConfig.getRequestsRoutingKey());
		return IntegrationFlows //
			.from(concurrentQueue())//
			.handle(File.class, this.s3UploadHandler) //
			.aggregate(this.aggregator) //
			.handle(Map.class, this.rmqProcessorAggregateArtifactsTransformer)//
			.handle(processorOutboundAdapter)//
			.get();
	}

	@Bean
	MessageChannel concurrentQueue() {
		var te = Executors.newFixedThreadPool(10);
		return MessageChannels.executor(te).get();
	}

	@Bean
	MessageChannel apiToPipelineChannel() {
		return MessageChannels.direct().get();
	}

}
