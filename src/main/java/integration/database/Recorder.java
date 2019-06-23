package integration.database;

import integration.AssetTypes;
import integration.PodcastPackageManifest;
import integration.aws.AwsS3Service;
import integration.events.PodcastArchiveUploadedEvent;
import integration.events.PodcastArtifactsUploadedToProcessorEvent;
import integration.events.PodcastProcessedEvent;
import integration.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Date;

@Log4j2
@Component
@RequiredArgsConstructor
@Transactional
class Recorder {

	private final AwsS3Service s3Service;

	private final PodcastRepository repository;

	@EventListener
	public void productionStartedForUpload(PodcastArchiveUploadedEvent uploadedEvent) {
		log.info("podcast archive has been uploaded: " + uploadedEvent.toString());
		var manifest = uploadedEvent.getSource();
		var podcast = Podcast.builder().date(new Date())
				.description(manifest.getDescription()).title(manifest.getTitle())
				.uid(manifest.getUid()).build();
		repository.save(podcast);

		var media = uploadedEvent.getSource().getMedia();
		if (!media.isEmpty()) {
			for (PodcastPackageManifest.Media m : media) {
				var extension = m.getExtension();

				var interviewMedia = Media.builder().fileName(m.getInterview())
						.extension(extension).type(AssetTypes.TYPE_INTERVIEW).build();
				var introMedia = Media.builder().extension(extension)
						.type(AssetTypes.TYPE_INTRODUCTION).fileName(m.getIntroduction())
						.build();
				if (podcast.getMedia() == null) {
					podcast.setMedia(new ArrayList<>());
				}
				podcast.getMedia().add(interviewMedia);
				podcast.getMedia().add(introMedia);
				repository.save(podcast);
			}
		}

		repository.save(podcast);

	}

	@EventListener
	public void artifactsUploadedToS3(PodcastArtifactsUploadedToProcessorEvent event) {

		// record data
		var files = event.getSource();
		var uid = files.getUid();
		repository.findByUid(uid).ifPresentOrElse(podcast -> {
			var fileMetadata = event.getSource();
			var uri = fileMetadata.getS3Uri();
			var type = fileMetadata.getType();
			podcast.getMedia().stream().filter(m -> m.getType().equalsIgnoreCase(type))
					.forEach(m -> m.setHref(uri));
			repository.save(podcast);
			log.info(event.getClass().getName() + " : " + "s3 artifact uploaded for file "
					+ fileMetadata.getType() + " for project with UID " + uid
					+ " which is an asset of type " + type);
		}, () -> log
				.info("there is no " + Podcast.class.getName() + " matching UID " + uid));

		// cleanup
		var stagingDirectory = event.getSource().getStagingDirectory();
		Assert.isTrue(
			!stagingDirectory.exists()
				|| FileUtils.deleteDirectoryRecursively(stagingDirectory),
			"we couldn't delete the staging directory. "
				+ "this could imperil our free space.");

	}

	@EventListener
	public void podcastProcessed(PodcastProcessedEvent event) {
		log.info("podcast audio file has been processed: " + event.toString());
		var uid = event.getUid();
		repository.findByUid(uid).ifPresentOrElse(p -> {
			var uri = s3Service.createS3Uri(event.getBucketName(), "",
					event.getFileName());
			p.setProductionArtifact(uri.toString());
			repository.save(p);
		}, () -> log.info(
				"there was no " + Podcast.class.getName() + " matching UID " + uid));
	}

}
