package pl;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "podcast.pipeline")
class PipelineProperties {

	private S3 s3 = new S3();
	private Processor processor = new Processor();

	@Data
	public static class S3 {

		private String bucketName;

		private File stagingDirectory;

	}


	@Data
	public static class Processor {

		private File inboundPodcastsDirectory;

		private String inputBucketName = "podcast-input-bucket";

		private String requestsQueue = "podcast-requests";

		private String requestsExchange = this.requestsQueue;

		private String requestsRoutingKey = this.requestsQueue;

		private String repliesQueue = "podcast-replies";

		private String repliesExchange = this.repliesQueue;

		private String repliesRoutingKey = this.repliesQueue;

	}

}
