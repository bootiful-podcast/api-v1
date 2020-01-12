package integration;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "podcast.pipeline")
public class PipelineProperties {

	private File root;

	private S3 s3 = new S3();

	private Processor processor = new Processor();

	private Notifications notifications = new Notifications();

	private Podbean podbean = new Podbean();

	@Data
	public static class Podbean {

		private String requestsQueue = "podbean-requests-queue";

		private String requestsExchange = this.requestsQueue;

		private String requestsRoutingKey = this.requestsQueue;

		private File podbeanDirectory;

	}

	@Data
	public static class Notifications {

		private String fromEmail;

		private String toEmail;

		private String subject;

	}

	@Data
	public static class S3 {

		private String inputBucketName, outputBucketName;

		private File stagingDirectory;

	}

	@Data
	public static class Processor {

		private File inboundPodcastsDirectory;

		private String inputBucketName = "podcast-input-bucket";

		// todo this needs to be changed here AND in the Python Processor code
		private String requestsQueue = "podcast-requests";

		private String requestsExchange = this.requestsQueue;

		private String requestsRoutingKey = this.requestsQueue;

		// todo this needs to be changed here AND in the Python Processor code
		private String repliesQueue = "podcast-replies";

		private String repliesExchange = this.repliesQueue;

		private String repliesRoutingKey = this.repliesQueue;

	}

}
