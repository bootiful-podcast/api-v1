package pl;

import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@EnableConfigurationProperties(PipelineProperties.class)
@SpringBootApplication
public class PipelineApplication {

	public static void main(String[] args) {
		SpringApplication.run(PipelineApplication.class, args);
	}

}

@Configuration
class ChannelsConfiguration {

	@Bean
	MessageChannel apiToPipelineChannel() {
		return MessageChannels.direct().get();
	}

}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
class UploadRequest {

	private File introduction, interview, manifest;

}

@Data
@AllArgsConstructor
@Log4j2
@NoArgsConstructor
class UploadPackageManifest {

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class Media {

		private String interview, introduction, extension;

	}

	private String description, uid;

	public Collection<Media> getMedia() {
		return new ArrayList<>(this.media);
	}

	private static String readAttributeFrom(String attr, NamedNodeMap map) {
		if (map == null || map.getLength() == 0) {
			return null;
		}
		var namedItem = map.getNamedItem(attr);
		if (namedItem != null) {
			var textContent = namedItem.getTextContent();
			if (StringUtils.hasText(textContent)) {
				return textContent;
			}
		}
		return null;
	}

	private Collection<Media> media = new ArrayList<>();

	private static Media readMedia(NodeList nodeList, String ext) {
		var ok = (nodeList != null && nodeList.getLength() > 0);
		if (!ok) {
			return null;
		}
		var first = nodeList.item(0);
		var attributes = first.getAttributes();
		var interview = readAttributeFrom("interview", attributes);
		var intro = readAttributeFrom("intro", attributes);
		return new Media(interview, intro, ext);
	}

	@SneakyThrows
	static UploadPackageManifest from(File manifest) {

		var dbf = DocumentBuilderFactory.newInstance();
		var db = dbf.newDocumentBuilder();
		var doc = db.parse(manifest);
		var build = new UploadPackageManifest();
		var podcast = doc.getElementsByTagName("podcast");
		Assert.isTrue(podcast.getLength() > 0,
			"there must be at least one podcast element in a manifest");
		var attributes = podcast.item(0).getAttributes();
		build.setDescription(readAttributeFrom("description", attributes));
		build.setUid(readAttributeFrom("uid", attributes));
		String[] exts = "mp3,wav".split(",");
		for (String ext : exts) {
			Media mediaFromDoc = getMediaFromDoc(doc, ext);
			if (null != mediaFromDoc) {
				build.media.add(mediaFromDoc);
			}
		}
		return build;
	}

	private static Media getMediaFromDoc(Document doc, String mp3Ext) {
		return readMedia(doc.getElementsByTagName(mp3Ext), mp3Ext);
	}

}

@Log4j2
@Configuration
class S3FlowConfiguration {

	private final PipelineProperties properties;

	private final ChannelsConfiguration channels;

	private final Function<File, Collection<Message<File>>> unzipSplitter;

	private final GenericHandler<File> s3UploadHandler;

	private final Consumer<AggregatorSpec> aggregator;

	S3FlowConfiguration(PipelineProperties properties, AwsS3Service s3,
																					ChannelsConfiguration channels) {
		this.properties = properties;
		this.channels = channels;
		this.unzipSplitter = (file) -> {
			var dest = new File(properties.getS3().getStagingDirectory(),
				UUID.randomUUID().toString());
			var files = Unzipper.unzip(file, dest);
			var manifest = files.stream()
				.filter(fn -> fn.getName().toLowerCase().endsWith("manifest.xml"))
				.collect(Collectors.toList());
			Assert.isTrue(manifest.size() > 0,
				"at least one file must be a manifest.xml file for a package to be considered valid.");
			var manifestFile = manifest.iterator().next();
			Assert.notNull(manifest, "the manifest must not be null");
			var upm = UploadPackageManifest.from(manifestFile);

			return files.stream().map(f -> {
				var builder = MessageBuilder//
					.withPayload(f)//
					.setHeader(Headers.CONTENT_TYPE, determineContentTypeFor(f))//
					.setHeader(Headers.PACKAGE_MANIFEST, upm);
				upm.getMedia().forEach(media -> {
					var interview = media.getInterview();
					var introduction = media.getIntroduction();
					Map.of(Headers.IS_INTERVIEW_FILE, f.getName().contains(interview),
						Headers.IS_INTRODUCTION_FILE,
						f.getName().contains(introduction)) //
						.forEach(builder::setHeader);

				});
				return builder.build();
			})//
				.collect(Collectors.toList());
		};
		this.s3UploadHandler = (file, messageHeaders) -> {
			var contentType = messageHeaders.get(Headers.CONTENT_TYPE, String.class);
			var manifest = messageHeaders.get(Headers.PACKAGE_MANIFEST,
				UploadPackageManifest.class);
			var s3Path = s3.upload(contentType, manifest.getUid(), file);
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
					UploadPackageManifest.class);
				var uid = Objects.requireNonNull(manifest).getUid();
				request.put("uid", uid);
			});

			return request;
		});

	}

	private void establishHeaderIfMatches(HashMap<String, String> request, Message<?> msg,
																																							String header, String newKey) {
		if (isTrue(msg.getHeaders(), header)) {
			request.put(newKey, msg.getHeaders().get(Headers.S3_PATH, String.class));
		}
	}
	// todo there's got to be a better way to do this.
	// todo wasn't there a handler thing in Java itself?

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

	//	@Bean
	IntegrationFlow audioProcessorPreparationPipeline(RabbitHelper helper,
																																																			AmqpTemplate template) {

		var processorConfig = properties.getProcessor();
		helper.defineDestination(processorConfig.getRequestsExchange(),
			processorConfig.getRequestsQueue(),
			processorConfig.getRequestsRoutingKey());

		var processorOutboundAdapter = Amqp.outboundAdapter(template)
			.exchangeName(processorConfig.getRequestsExchange())
			.routingKey(processorConfig.getRequestsRoutingKey());

		return IntegrationFlows//
			.from(this.channels.apiToPipelineChannel()) //
			.split(File.class, this.unzipSplitter) //
			.handle(File.class, this.s3UploadHandler) //
			.aggregate(this.aggregator)//
			.handle(Map.class,
				(payload, headers) -> MessageBuilder.withPayload(payload)
					.setHeader(Headers.UID, payload.get(Headers.UID))
					.setHeader(Headers.PROCESSOR_REQUEST_INTERVIEW,
						payload.get(Headers.PROCESSOR_REQUEST_INTERVIEW))
					.setHeader(Headers.PROCESSOR_REQUEST_INTRODUCTION,
						payload.get(
							Headers.PROCESSOR_REQUEST_INTRODUCTION)))//
			.handle(processorOutboundAdapter)//
			.get();
	}

}

// todo remove this
@Deprecated
@Log4j2
@Configuration
class Demo {

	private final MessageChannel pipeline;

	Demo(ChannelsConfiguration channelsConfiguration, Environment e) {
		this.pipeline = channelsConfiguration.apiToPipelineChannel();
		log.info("the message is: 'hello..." + e.getProperty("hello") + "'!");
	}

	//@EventListener(ApplicationReadyEvent.class)
	public void go() {
		var msg = MessageBuilder
			.withPayload("/Users/joshlong/Desktop/sample-package.zip".trim())//
			.setHeader(Headers.PACKAGE_ID, UUID.randomUUID().toString())//
			.build();
		Assert.isTrue(this.pipeline.send(msg),
			"the production pipeline process couldn't be started.");
	}

}

abstract class FileUtils {

	static File assertFileExists(File f) {
		Assert.notNull(f, "you must provide a non-null argument");
		Assert.isTrue(f.exists(), "the file should exist at " + f.getAbsolutePath());
		return f;
	}

	static File ensureDirectoryExists(File f) {
		Assert.notNull(f, "you must provide a non-null argument");
		Assert.isTrue(f.exists() || f.mkdirs(),
			"the file " + f.getAbsolutePath() + " does not exist");
		return f;
	}

}

@Log4j2
@RestController
class PackageUploadController {

	private final File file;

	private final MessageChannel apiToPipelineChannel;

	PackageUploadController(PipelineProperties props, ChannelsConfiguration channels) {
		this.file = FileUtils.ensureDirectoryExists(props.getS3().getStagingDirectory());
		this.apiToPipelineChannel = channels.apiToPipelineChannel();
	}

	@PostMapping("/production")
	ResponseEntity<?> beginProduction(@RequestParam("id") String id,
																																			@RequestParam("file") MultipartFile file) throws Exception {
		var newFile = new File(this.file, id);
		file.transferTo(newFile);
		FileUtils.assertFileExists(newFile);
		var msg = MessageBuilder.withPayload(newFile).setHeader(Headers.PACKAGE_ID, id)
			.build();
		this.apiToPipelineChannel.send(msg);
		return ResponseEntity.accepted().body(Map.of("status", "OK"));
	}

}
