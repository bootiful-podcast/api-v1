package integration.events;

import org.springframework.context.ApplicationEvent;

import java.net.URI;
import java.util.Map;

public class PodcastPublishedToPodbeanEvent extends ApplicationEvent {

	public PodcastPublishedToPodbeanEvent(String uid, URI mediaUrl, URI permalinkUrl) {
		super(Map.of("uid", uid, "mediaUrl", mediaUrl, "playerUrl", permalinkUrl));
	}

}
