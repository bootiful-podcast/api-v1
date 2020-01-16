package integration.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

import java.io.File;

public class PodcastArtifactsUploadedToProcessorEvent extends ApplicationEvent {

	public PodcastArtifactsUploadedToProcessorEvent(String uid, String type, String s3Uri,
			File stagingDirectory) {
		super(PodcastFiles//
				.builder() //
				.uid(uid) //
				.file(stagingDirectory) //
				.type(type) //
				.s3Uri(s3Uri) //
				.build() //
		);
	}

	@Override
	public PodcastFiles getSource() {
		return (PodcastFiles) super.getSource();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class PodcastFiles {

		private String uid, type, s3Uri;

		private File file;

	}

}
