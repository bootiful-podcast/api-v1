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

	@Data
	public static class S3 {

		private String bucketName;

		private File stagingDirectory;

	}

}
