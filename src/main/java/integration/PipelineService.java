package integration;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.io.File;

class PipelineService {

	private final MessageChannel pipeline;

	PipelineService(MessageChannel channel) {
		this.pipeline = channel;
	}

	public boolean launchPipeline(String uid, File archiveFromClientContainingPodcastAssets) {
		var msg = MessageBuilder //
				.withPayload(archiveFromClientContainingPodcastAssets.getAbsolutePath())//
				.setHeader(Headers.UID, uid)//
				.build();
		return this.pipeline.send(msg);
	}

}
