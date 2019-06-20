package pl;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import pl.aws.AwsS3Service;
import pl.events.PodcastArchiveUploadedEvent;
import pl.events.PodcastArtifactsUploadedToProcessorEvent;
import pl.events.PodcastProcessedEvent;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Configuration
class AwsS3FlowConfiguration {

	private final PipelineProperties properties;

	private final Json json;

	private final ChannelsConfiguration channels;

	private final Function<File, Collection<Message<File>>> unzipSplitter;

	private final GenericHandler<File> s3UploadHandler;

	private final Consumer<AggregatorSpec> aggregator;

	private final GenericHandler<Map> rmqProcessorAggregateArtifactsTransformer;

	private final ApplicationEventPublisher publisher;

	AwsS3FlowConfiguration(PipelineProperties properties, Json jsonService,
			AwsS3Service s3, ChannelsConfiguration channels,
			ApplicationEventPublisher publisher) {
		this.properties = properties;
		this.publisher = publisher;
		this.channels = channels;
		this.json = jsonService;
		this.unzipSplitter = (file) -> {
			var stagingDirectoryForRequest = FileUtils.ensureDirectoryExists(
					new File(properties.getS3().getStagingDirectory(),
							UUID.randomUUID().toString()));

			var files = Unzipper.unzip(file, stagingDirectoryForRequest);
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
						.setHeader(Headers.CONTENT_TYPE, determineContentTypeFor(f))//
						.setHeader(Headers.PACKAGE_MANIFEST, uploadPackageManifest);

				uploadPackageManifest.getMedia().forEach(media -> {

					var interview = f.getName().contains(media.getInterview());
					var intro = f.getName().contains(media.getIntroduction());
					var mediaMap = Map.of( //
							Headers.IS_INTERVIEW_FILE,
							f.getName().contains(media.getInterview()), //
							Headers.IS_INTRODUCTION_FILE,
							f.getName().contains(media.getIntroduction()) //
					);
					mediaMap.forEach(builder::setHeader);
					var type = interview ? MediaTypes.TYPE_INTERVIEW
							: (intro ? MediaTypes.TYPE_INTRODUCTION : null);
					if (StringUtils.hasText(type)) {
						builder.setHeader(Headers.ASSET_TYPE, type);
					}
				});
				return builder.build();
			});

			return stream.collect(Collectors.toList());
		};
		this.s3UploadHandler = (file, messageHeaders) -> {
			var contentType = messageHeaders.get(Headers.CONTENT_TYPE, String.class);
			var manifest = messageHeaders.get(Headers.PACKAGE_MANIFEST,
					PodcastPackageManifest.class);
			var uid = manifest.getUid();
			var s3Path = s3.upload(contentType, uid, file);
			var role = messageHeaders.get(Headers.ASSET_TYPE, String.class);
			publisher.publishEvent(
					new PodcastArtifactsUploadedToProcessorEvent(uid, role, s3Path));
			return MessageBuilder //
					.withPayload(file) //
					.setHeader(Headers.S3_PATH, s3Path) //
					.build();
		};
		this.aggregator = spec -> spec.outputProcessor(group -> {
			var messages = group.getMessages();
			var request = new HashMap<String, String>();
			messages.forEach(msg -> {
				establishHeaderIfMatches(request, msg, Headers.IS_INTRODUCTION_FILE,
						Headers.PROCESSOR_REQUEST_INTRODUCTION);
				establishHeaderIfMatches(request, msg, Headers.IS_INTERVIEW_FILE,
						Headers.PROCESSOR_REQUEST_INTERVIEW);
				var manifest = msg.getHeaders().get(Headers.PACKAGE_MANIFEST,
						PodcastPackageManifest.class);
				var uid = Objects.requireNonNull(manifest).getUid();
				request.put("uid", uid);
			});
			return request;
		});
		this.rmqProcessorAggregateArtifactsTransformer = (payload, headers) -> {
			var json = jsonService.toJson(payload);
			return MessageBuilder //
					.withPayload(json) //
					.setHeader(Headers.UID, payload.get(Headers.UID)) //
					.setHeader(Headers.PROCESSOR_REQUEST_INTERVIEW,
							payload.get(Headers.PROCESSOR_REQUEST_INTERVIEW)) //
					.setHeader(Headers.PROCESSOR_REQUEST_INTRODUCTION,
							payload.get(Headers.PROCESSOR_REQUEST_INTRODUCTION)) //
					.build();
		};

	}

	private void establishHeaderIfMatches(HashMap<String, String> request, Message<?> msg,
			String header, String newKey) {
		if (isTrue(msg.getHeaders(), header)) {
			request.put(newKey, msg.getHeaders().get(Headers.S3_PATH, String.class));
		}
	}
	// todo there's got to be a better way to do this.
	// todo wasn't there a rmqProcessorAggregateArtifactsTransformer thing in Java itself?

	private static String determineContentTypeFor(File file) {
		Assert.notNull(file, "the file must not be null");
		var map = new HashMap<String, String>();
		map.put("wav", "audio/wav");
		map.put("mp3", "audio/mp3");
		map.put("xml", "application/xml");
		var fn = file.getName().toLowerCase();
		for (String ext : map.keySet()) {
			if (fn.endsWith(ext)) {
				return map.get(ext);
			}
		}
		throw new RuntimeException("Invalid file-type!");
	}

	private Boolean isTrue(MessageHeaders headers, String header) {
		return headers.get(header, Boolean.class);
	}

	@Bean
	IntegrationFlow audioProcessorReplyPipeline(ConnectionFactory connectionFactory) {
		var amqpInboundAdapter = Amqp.inboundAdapter(connectionFactory,
				properties.getProcessor().getRepliesQueue()).get();
		return IntegrationFlows.from(amqpInboundAdapter)
				.handle(String.class, (payload, headers) -> {
					var resultMap = json.fromJson(payload,
							new TypeReference<Map<String, String>>() {
							});
					var output = resultMap.getOrDefault("mp3",
							resultMap.getOrDefault("wav", null));
					recordProcessedFilesToDatabase(resultMap.get("uid"),
							resultMap.get("output-bucket-name"), output);
					return null;
				}).get();
	}

	private void recordUploadPackageManifest(PodcastPackageManifest packageManifest) {
		this.publisher.publishEvent(new PodcastArchiveUploadedEvent(packageManifest));
	}

	private void recordProcessedFilesToDatabase(String uid, String outputBucketName,
			String fileName) {
		this.publisher
				.publishEvent(new PodcastProcessedEvent(uid, outputBucketName, fileName));
	}

	@Bean
	IntegrationFlow audioProcessorPreparationPipeline(RabbitHelper helper,
			AmqpTemplate template) {

		var processorConfig = properties.getProcessor();
		helper.defineDestination(processorConfig.getRequestsExchange(),
				processorConfig.getRequestsQueue(),
				processorConfig.getRequestsRoutingKey());

		var processorOutboundAdapter = Amqp //
				.outboundAdapter(template)//
				.exchangeName(processorConfig.getRequestsExchange()) //
				.routingKey(processorConfig.getRequestsRoutingKey());

		return IntegrationFlows//
				.from(this.channels.apiToPipelineChannel()) //
				.split(File.class, this.unzipSplitter) //
				.handle(File.class, this.s3UploadHandler) //
				.aggregate(this.aggregator)//
				.handle(Map.class, this.rmqProcessorAggregateArtifactsTransformer)//
				.handle(processorOutboundAdapter)//
				.get();
	}

}
