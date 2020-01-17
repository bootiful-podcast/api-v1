package integration;

import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.self.ServerUriResolver;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.io.File;
import java.net.URI;
import java.util.Optional;

class PipelineService {

	private final MessageChannel pipeline;

	private final PodcastRepository repository;

	private final ServerUriResolver resolver;

	PipelineService(MessageChannel channel, PodcastRepository repository,
			ServerUriResolver resolver) {
		this.pipeline = channel;
		this.repository = repository;
		this.resolver = resolver;
	}

	public boolean launchPipeline(String uid,
			File archiveFromClientContainingPodcastAssets) {
		var msg = MessageBuilder //
				.withPayload(archiveFromClientContainingPodcastAssets.getAbsolutePath())//
				.setHeader(Headers.UID, uid)//
				.build();
		return this.pipeline.send(msg);
	}

	private URI uriFromPodcast(URI server, Optional<Podcast> podcast) {
		var path = podcast//
				.map(p -> "/podcasts/" + p.getUid() + "/produced-audio")//
				.orElseThrow(() -> new IllegalArgumentException(
						"you must provide a valid podcast identifier"));//
		return URI.create(server.toString() + path);
	}

	public URI buildMediaUriForPodcastById(Long podcastid) {
		URI uri = this.resolver.resolveCurrentRootUri();
		return this.uriFromPodcast(uri, this.repository.findById(podcastid));
	}

}
