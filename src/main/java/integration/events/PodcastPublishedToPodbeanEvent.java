package integration.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

import java.net.URI;

public class PodcastPublishedToPodbeanEvent extends ApplicationEvent {

	public PodcastPublishedToPodbeanEvent(String uid, URI mediaUrl, URI permalinkUrl) {
		super(PodbeanPodcast.builder().uid(uid).mediaUrl(mediaUrl).playerUrl(permalinkUrl)
				.build());
	}

	@Override
	public PodbeanPodcast getSource() {
		return (PodbeanPodcast) super.getSource();
	}

	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class PodbeanPodcast {

		private String uid;

		private URI mediaUrl, playerUrl;

	}

}
