package integration.soundcloud.migration;

import integration.AssetTypes;
import integration.PipelineProperties;
import integration.PreproducedPodcastPackageManifest;
import integration.aws.AwsS3Service;
import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.events.PodcastArchiveUploadedEvent;
import integration.events.PodcastArtifactsUploadedToProcessorEvent;
import integration.utils.CopyUtils;
import integration.utils.JsonHelper;
import integration.utils.PipelineUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Configuration
@Profile("migration")
@RequiredArgsConstructor
class MigrationJob {

	private final JsonHelper jsonHelper;

	private final RetryTemplate retryTemplate = new RetryTemplate();

	private final ApplicationEventPublisher publisher;

	private final AwsS3Service awsS3Service;

	private final PodcastRepository repository;

	private final TransactionTemplate transactionTemplate;

	private final JdbcTemplate template;

	private final PipelineProperties properties;

	private boolean shouldHandleDirectory(File file) {
		var img = new File(file, "image.jpg");
		var lengthLessThan1Mb = (img.length() < (1000 * 1000));
		log.info("we're returning " + lengthLessThan1Mb + " for " + file.getAbsolutePath() + ".");
		return lengthLessThan1Mb;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void run() {
		this.reset();
		var outboundQueue = onwardToTheReplyQueue();
		Stream.of(Objects.requireNonNull(MigrationUtils.getRoot().listFiles(
				directory -> directory.isDirectory() && directory.exists() && shouldHandleDirectory(directory))))
				.map(f -> this.installPodcastRecords(outboundQueue, f)).peek(log::info)
				.forEach(p -> this.transactionTemplate.executeWithoutResult(status -> log.info(p)));
	}

	@SneakyThrows
	private void reset() {

		/*
		// reset the DB
		 Arrays.asList("delete from podcast_link; delete from podcast_media; delete from
		  podcast; delete from media; delete from link; delete from
		  mappings".split(";")).forEach(template::update);
 	*/
		// reset the file system
		var desktop = new File(System.getProperty("user.home"), "Desktop");
		var soundcloud = new File(desktop, "soundcloud");
		var backup = new File(desktop, "soundcloud-full-backup");
		Stream.of(desktop, backup)
				.forEach(f -> Assert.isTrue(f.exists(), f.getAbsolutePath() + " does not exist, but should"));
		Assert.isTrue(!soundcloud.exists() || FileSystemUtils.deleteRecursively(soundcloud),
				"the directory " + soundcloud.getAbsolutePath() + " could not be deleted.");
		FileSystemUtils.copyRecursively(backup, soundcloud);
		Assert.isTrue(soundcloud.exists(),
				"the soundcloud directory does not exist, and so processing should terminate.");
	}

	@Data
	@Builder
	@RequiredArgsConstructor
	private static class Upload {

		private final String assetType;

		private final File file;

		private final MediaType mediaType;

	}

	@Bean
	IntegrationFlow sendFinishedPodcastsToTheRestOfThePipeline(RabbitTemplate rabbitTemplate) {
		return IntegrationFlows.from(this.onwardToTheReplyQueue())
				.handle(Amqp.outboundAdapter(rabbitTemplate)
						.exchangeName(this.properties.getProcessor().getRepliesQueue())
						.routingKey(this.properties.getProcessor().getRepliesRoutingKey()))
				.get();
	}

	private Map<String, Object> replyMessage(String uid) {
		return Map.of("output-bucket-name", properties.getS3().getOutputBucketName(), //
				"uid", uid, //
				"mp3", uid + ".mp3" //
		);
	}

	@Bean
	MessageChannel onwardToTheReplyQueue() {
		return MessageChannels.direct().get();
	}

	private static File relocate(File from, String uid) {
		Assert.isTrue(from.exists(), "the source of the relcoation, " + from.getAbsolutePath() + ", does not exist");
		File target = new File(from.getParentFile(), uid + '.' + CopyUtils.extensionFor(from));
		CopyUtils.copy(from, target);
		Assert.isTrue(target.exists(), "the target file, " + target.getAbsolutePath() + ", does not exist");
		Assert.isTrue(from.delete() || !from.exists(),
				"the src file, " + from.getAbsolutePath() + ", should have been deleted");
		log.info("relocating " + from.getAbsolutePath() + " to " + target.getAbsolutePath());
		return target;
	}

	private String getUidForGuid(String guid) {
		var listOfMaps = this.template.queryForList("select uid, json_guid from mappings where json_guid = ? ", guid)
				.stream()//
				.map(m -> (String) m.get("uid"))//
				.collect(Collectors.toList());
		return listOfMaps.size() > 0 ? listOfMaps.get(0) : null;
	}

	@SneakyThrows
	private Podcast installPodcastRecords(MessageChannel messageChannel, File folder) {

		var json = new File(folder, "podcast.json");
		var soundCloudPodcast = (SoundcloudPodcast) this.jsonHelper
				.fromJson(FileCopyUtils.copyToString(new FileReader(json)), SoundcloudPodcast.class);
		var existingGuid = soundCloudPodcast.getGuid();

		if ((getUidForGuid(existingGuid)) != null) {
			log.info("there is already a record for UID " + existingGuid + " so we will not attempt to migrate it. "
					+ "If you worry it's in an inconsistent state, delete all the data.");
			return null;
		}

		this.template.update("INSERT INTO mappings(uid, json_guid) VALUES(?,?) ON CONFLICT DO NOTHING ",
				UUID.randomUUID().toString(), existingGuid);

		var uid = getUidForGuid(existingGuid);
		var audio = relocate(new File(folder, "audio.mp3"), uid);
		var image = relocate(new File(folder, "image.jpg"), uid);

		if (!(audio.exists() && image.exists() && json.exists())) {
			log.warn("could not process " + folder.getAbsolutePath()
					+ " because we are missing some of the required files for processing.");
			return null;
		}

		log.info("the UID is " + uid + " and it's linked to " + existingGuid);
		var uploadPackageManifest = PreproducedPodcastPackageManifest.from(uid, soundCloudPodcast.getTitle(),
				soundCloudPodcast.getDescription(), audio.getName(), image.getName());
		this.publisher.publishEvent(new PodcastArchiveUploadedEvent(uploadPackageManifest));
		return PipelineUtils.podcastOrElseThrow(uid, this.repository.findByUid(uid).map(podcast -> {
			podcast.setDate(soundCloudPodcast.getPubDate());
			repository.save(podcast);

			log.info("there is no Podcast with the UID " + uid + ", so we'll create it.");
			var uploads = List.of(new Upload(AssetTypes.TYPE_PHOTO, image, MediaType.IMAGE_JPEG),
					new Upload(AssetTypes.TYPE_PRODUCED_AUDIO, audio, MediaType.parseMediaType("audio/mpeg3")));
			uploads.forEach(upload -> {
				this.retryTemplate.execute(context -> this.awsS3Service
						.uploadOutputFile(upload.getMediaType().toString(), uid, upload.getFile()));
				var s3Uri = this.retryTemplate.execute(context -> this.awsS3Service
						.uploadInputFile(upload.getMediaType().toString(), uid, upload.getFile()));
				this.publisher.publishEvent(new PodcastArtifactsUploadedToProcessorEvent(uid, upload.getAssetType(),
						s3Uri.toString(), upload.getFile()));
			});
			var mapAsAString = this.jsonHelper.toJson(replyMessage(uid));
			var sent = messageChannel.send(MessageBuilder.withPayload(mapAsAString).build());
			log.info("sending the following message to the next stage in the pipeline " + mapAsAString
					+ ". Was it sent? " + sent);
			return podcast;
		}));
	}

}
