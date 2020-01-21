package integration.database;

import integration.AssetTypes;
import integration.aws.AwsS3Service;
import integration.events.PodcastArchiveUploadedEvent;
import integration.events.PodcastArtifactsUploadedToProcessorEvent;
import integration.events.PodcastProcessedEvent;
import integration.events.PodcastPublishedToPodbeanEvent;
import integration.utils.CopyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

@Log4j2
@Component
@RequiredArgsConstructor
@Transactional
class Recorder {

	private final AwsS3Service s3Service;

	private final PodcastRepository repository;

	private static String[] baseAndExtension(String fileName) {
		if (fileName.contains(".")) {
			return fileName.split("\\.");
		}
		return null;
	}

	private static Runnable missingPodcastRunnable(String uid) {
		return () -> log.info("there is no " + Podcast.class.getName() + " matching UID " + uid);
	}

	private Media mediaFor(String fn, String at) {
		var split = baseAndExtension(fn);
		return Media.builder().fileName(fn).extension(Objects.requireNonNull(split)[1]).type(at).build();
	}

	@EventListener
	public void productionStartedForUpload(PodcastArchiveUploadedEvent uploadedEvent) {
		log.info("podcast archive has been uploaded: " + uploadedEvent.toString());
		var manifest = uploadedEvent.getSource();
		var podcast = Podcast.builder().date(new Date()).description(manifest.getDescription())
				.title(manifest.getTitle()).uid(manifest.getUid()).build();
		repository.save(podcast);
		var interviewMedia = mediaFor(manifest.getInterview().getSrc(), AssetTypes.TYPE_INTERVIEW);
		var introMedia = mediaFor(manifest.getIntroduction().getSrc(), AssetTypes.TYPE_INTRODUCTION);
		var photoMedia = mediaFor(manifest.getPhoto().getSrc(), AssetTypes.TYPE_PHOTO);
		if (podcast.getMedia() == null) {
			podcast.setMedia(new ArrayList<>());
		}
		Arrays.asList(interviewMedia, introMedia, photoMedia).forEach(m -> podcast.getMedia().add(m));
		repository.save(podcast);
	}

	@EventListener
	public void artifactsUploadedToS3(PodcastArtifactsUploadedToProcessorEvent event) {
		// record data
		var fileMetadata = event.getSource();
		var uid = fileMetadata.getUid();
		var type = fileMetadata.getType();
		var uri = fileMetadata.getS3Uri();

		this.repository.findByUid(uid).ifPresentOrElse(podcast -> {
			if (type != null) {
				// happens when the file is manifest.xml,
				// which we don't really need to preserve.
				if (type.equalsIgnoreCase(AssetTypes.TYPE_PHOTO)) {
					podcast.setS3PhotoUri(uri);
					podcast.setS3PhotoFileName(fileMetadata.getFile().getName());
				}
				podcast.getMedia().stream().filter(m -> m.getType().equalsIgnoreCase(type))
						.forEach(m -> m.setHref(uri));
				this.repository.save(podcast);
				log.info(event.getClass().getName() + " : " + "s3 artifact uploaded for file " + fileMetadata.getType()
						+ " for project with UID " + uid + " which is an asset of type " + type);
			}
		}, missingPodcastRunnable(uid));

		var stagingDirectory = event.getSource().getFile();
		Assert.isTrue(!stagingDirectory.exists() || CopyUtils.deleteDirectoryRecursively(stagingDirectory),
				"We couldn't delete the staging directory. This could imperil our free space.");
	}

	@EventListener
	public void podcastProcessed(PodcastProcessedEvent event) {
		log.info("podcast audio file has been processed: " + event.toString());
		var uid = event.getUid();
		repository.findByUid(uid).ifPresentOrElse(podcast -> {
			var uri = s3Service.createS3Uri(event.getBucketName(), "", event.getFileName());
			podcast.setS3AudioUri(uri.toString());
			podcast.setS3AudioFileName(event.getFileName());
			repository.save(podcast);
		}, missingPodcastRunnable(uid));
	}

	@EventListener
	public void podcastPublishedToPodbean(PodcastPublishedToPodbeanEvent event) {
		var uid = event.getSource().getUid();
		repository.findByUid(uid).ifPresentOrElse(podcast -> {
			podcast.setPodbeanDraftCreated(new Date());
			podcast.setPodbeanMediaUri(event.getSource().getMediaUrl().toString());
			repository.save(podcast);
		}, missingPodcastRunnable(uid));
	}

}
