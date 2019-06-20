package integration.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

public class PodcastArtifactsUploadedToProcessorEvent extends ApplicationEvent {

	@Override
	public PodcastFiles getSource() {
		return (PodcastFiles) super.getSource();
	}

	public PodcastArtifactsUploadedToProcessorEvent(String uid, String type,
			String s3Uri) {
		super(PodcastFiles.builder().uid(uid).type(type).s3Uri(s3Uri).build());
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class PodcastFiles {

		private String uid, type, s3Uri;

	}

}
