package integration;

import com.amazonaws.services.s3.model.S3Object;
import integration.aws.AwsS3Service;
import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.self.ServerUriResolver;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

class PipelineService {

	private final MessageChannel pipeline;

	private final AwsS3Service s3;

	private final PodcastRepository repository;

	private final ServerUriResolver resolver;

	PipelineService(MessageChannel channel, AwsS3Service s3, PodcastRepository repository, ServerUriResolver resolver) {
		this.pipeline = channel;
		this.s3 = s3;
		this.repository = repository;
		this.resolver = resolver;
	}

	public Resource getPodcastPhotoMedia(String uid) {
		return read(uid, podcast -> podcast.getUid() + ".jpg", fn -> this.s3.downloadOutputFile(uid, fn));
	}

	public Resource getPodcastAudioMedia(String uid) {
		return read(uid, podcast -> podcast.getUid() + ".mp3", fn -> this.s3.downloadOutputFile(uid, fn));
	}

	private Resource read(String uid, Function<Podcast, String> functionToExtractAFileNameKeyGivenAPodcast,
			Function<String, S3Object> produceS3Object) {
		return this.repository//
				.findByUid(uid)//
				.map(functionToExtractAFileNameKeyGivenAPodcast) //
				.map(s3Key -> {
					try {
						return new Object() {
							S3Object object = produceS3Object.apply(s3Key);

							String key = s3Key;

						};
					}
					catch (Exception e) {
						ReflectionUtils.rethrowRuntimeException(e);
					}
					return null;
				})//
				.map(record -> {
					var inputStream = record.object.getObjectContent();
					return new InputStreamResource(inputStream);
				}) //
				.orElseThrow(
						() -> new IllegalArgumentException("couldn't find the Podcast associated with UID  " + uid));

	}

	public boolean launchPipeline(String uid, File archiveFromClientContainingPodcastAssets) {
		var msg = MessageBuilder //
				.withPayload(archiveFromClientContainingPodcastAssets.getAbsolutePath())//
				.setHeader(Headers.UID, uid)//
				.build();
		return this.pipeline.send(msg);
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
