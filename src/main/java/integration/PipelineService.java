package integration;

import com.amazonaws.services.s3.model.S3Object;
import integration.aws.AwsS3Service;
import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.self.ServerUriResolver;
import lombok.Data;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

public class PipelineService {

	private final MessageChannel fastTrackMessageChannel, fullPipelineMessageChannel;

	private final AwsS3Service s3;

	private final PodcastRepository repository;

	private final ServerUriResolver resolver;

	PipelineService(MessageChannel fullPipelineMessageChannel, MessageChannel fastTrackMessageChannel, AwsS3Service s3,
			PodcastRepository repository, ServerUriResolver resolver) {
		this.fullPipelineMessageChannel = fullPipelineMessageChannel;
		this.fastTrackMessageChannel = fastTrackMessageChannel;
		this.s3 = s3;
		this.repository = repository;
		this.resolver = resolver;
	}

	public S3Resource getPodcastPhotoMedia(String uid) {
		return buildResourceFor(uid, podcast -> podcast.getUid() + ".jpg", fn -> this.s3.downloadOutputFile(uid, fn));
	}

	public S3Resource getPodcastAudioMedia(String uid) {
		return buildResourceFor(uid, podcast -> podcast.getUid() + ".mp3", fn -> this.s3.downloadOutputFile(uid, fn));
	}

	@Data
	public static class S3Resource {

		private final Resource resource;

		private final long length;

	}

	private S3Resource buildResourceFor(String uid,
			Function<Podcast, String> functionToExtractAFileNameKeyGivenAPodcast,
			Function<String, S3Object> produceS3Object) {
		return this.repository//
				.findByUid(uid)//
				.map(functionToExtractAFileNameKeyGivenAPodcast) //
				.map(produceS3Object)//
				.map(record -> new S3Resource(new InputStreamResource(record.getObjectContent()),
						record.getObjectMetadata().getContentLength()))//
				.orElseThrow(
						() -> new IllegalArgumentException("couldn't find the Podcast associated with UID  " + uid));

	}

	/**
	 * This pipeline expects an archive, containing an interview {@code mp3}, an
	 * introduction {@code mp3}, {@code jpg}, and a {@code manifest.xml}, for a given
	 * {@code uid}. There are the ingredients necessary to produce a podcast from the very
	 * beginning.
	 */
	public boolean launchProcessorPipeline(String uid, File archiveFromClientContainingPodcastAssets) {
		var msg = MessageBuilder //
				.withPayload(archiveFromClientContainingPodcastAssets.getAbsolutePath())//
				.setHeader(Headers.UID, uid)//
				.build();
		return this.fullPipelineMessageChannel.send(msg);
	}

	/**
	 * <p>
	 * Now, I want to publish the episodes into the new pipeline. But we need to take
	 * advantage of only the part of the process after the message has returned from the
	 * Python processor.
	 * <p>
	 * The pre-requirements:
	 * <OL>
	 * <LI>the image and the file must be uploaded already to the output bucket on S3</LI>
	 * <LI>a Podcast record must have been recorded in the DB. (see the various event
	 * handlers triggered in Step1UploadPreparationIntegrationConfiguration)</LI>
	 * <LI>a new message needs to arrive on the right message queue so as to trigger
	 * Step2ProcessorReplyIntegrationConfiguration</LI>
	 * </OL>
	 */
	public boolean launchPublicationPipline(String uid, File producedAudio, File episodePhoto, String title,
			String description) {
		var producedPodcast = new ProducedPodcast(uid, title, description, producedAudio, episodePhoto);
		var msg = MessageBuilder.withPayload(producedPodcast).setHeader(Headers.UID, uid).build();
		return this.fastTrackMessageChannel.send(msg);
	}

	private URI uriFromPodcast(URI server, Optional<Podcast> podcast) {
		var path = podcast//
				.map(p -> "/podcasts/" + p.getUid() + "/produced-audio")//
				.orElseThrow(() -> new IllegalArgumentException("you must provide a valid podcast identifier"));//
		return URI.create(server.toString() + path);
	}

	public URI buildMediaUriForPodcastById(Long podcastid) {
		URI uri = this.resolver.resolveCurrentRootUri();
		return this.uriFromPodcast(uri, this.repository.findById(podcastid));
	}

}
