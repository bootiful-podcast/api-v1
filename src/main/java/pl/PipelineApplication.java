package pl;

import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
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
	private static class Media {

		private String interview, introduction, extension;

	}

	private String description, uid;

	public Media[] getMedia() {
		return this.media.toArray(new Media[0]);
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
		var intro = readAttributeFrom("introduction", attributes);
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

	private final AwsS3Service s3;

	private final ChannelsConfiguration channels;

	private final Function<File, Collection<Message<File>>> unzipSplitter;

	private final GenericHandler<File> s3UploadHandler;

	S3FlowConfiguration(PipelineProperties properties, AwsS3Service s3,
			ChannelsConfiguration channels) {
		this.properties = properties;
		this.s3 = s3;
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
			return files.stream().map(f -> MessageBuilder//
					.withPayload(f)//
					.setHeader(Headers.CONTENT_TYPE, determineContentTypeFor(f))//
					.setHeader(Headers.PACKAGE_MANIFEST, upm)//
					.build()).collect(Collectors.toList());
		};
		this.s3UploadHandler = (file, messageHeaders) -> {
			var contentType = determineContentTypeFor(file);
			var manifest = (UploadPackageManifest) messageHeaders
					.get(Headers.PACKAGE_MANIFEST);
			var s3Path = s3.upload(contentType, manifest.getUid(), file);
			return MessageBuilder.withPayload(file) //
					.setHeader(Headers.S3_PATH, s3Path) //
					.build();
		};
	}

	// todo there's got to be a better way to do this.
	// todo wasn't there a handler thing in Java itself?

	private static String determineContentTypeFor(File file) {
		Assert.notNull(file, "the file must not be null");
		var fn = file.getName().toLowerCase();
		if (fn.endsWith(".wav"))
			return "audio/wav";
		if (fn.endsWith(".mp3"))
			return "audio/mp3";
		if (fn.endsWith(".xml"))
			return "application/xml";
		throw new RuntimeException("Invalid file-type!");
	}

	@Bean
	IntegrationFlow unzipPipeline() {
		return IntegrationFlows//
				.from(this.channels.apiToPipelineChannel()) //
				.split(File.class, this.unzipSplitter) //
				.handle(File.class, this.s3UploadHandler) //
				.handle(File.class, (file, messageHeaders) -> {
					log.info("----------------------------------");
					log.info("payload:  " + file.getAbsolutePath());
					messageHeaders.forEach((k, v) -> log.info(k + '=' + v));
					return null;
				}).get();
	}

}

// todo remove this
@Deprecated
@Configuration
class Demo {

	private final MessageChannel pipeline;

	Demo(ChannelsConfiguration channelsConfiguration) {
		this.pipeline = channelsConfiguration.apiToPipelineChannel();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void go() {
		var msg = MessageBuilder
				.withPayload("/Users/joshlong/Desktop/sample-package.zip".trim())//
				.setHeader(Headers.PACKAGE_ID, UUID.randomUUID().toString())//
				.build();
		Assert.isTrue(this.pipeline.send(msg),
				"the production process should have been started by now.");
	}

}

abstract class FileUtils {

	static File assertFileExists(File f) {
		Assert.isTrue(f.exists(), "the file should exist at " + f.getAbsolutePath());
		return f;
	}

	static File ensureDirectoryExists(File f) {
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
