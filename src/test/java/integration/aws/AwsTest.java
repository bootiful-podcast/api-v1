package integration.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import integration.PipelineProperties;
import integration.utils.CopyUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.io.File;
import java.util.Arrays;

public class AwsTest {

	private final Resource resource = new ClassPathResource("/sample-image.jpg");

	@Test
	public void uploadAndDownload() throws Exception {
		var sampleImageBeforeUpload = File.createTempFile("sample-image-before-upload",
				".jpg");
		var sampleImageAfterUpload = File.createTempFile("sample-image-after-upload",
				".jpg");

		try {
			var amazonS3 = amazonS3(System.getenv("AWS_ACCESS_KEY_ID"),
					System.getenv("AWS_SECRET_ACCESS_KEY"), System.getenv("AWS_REGION"));
			var amazonS3Service = new AwsS3Service("podcast-input-bucket",
					"podcast-output-bucket", amazonS3);

			CopyUtils.copy(this.resource.getInputStream(), sampleImageBeforeUpload);
			amazonS3Service.uploadInputFile(MediaType.IMAGE_JPEG_VALUE, "test",
					sampleImageBeforeUpload);
			var s3Object = amazonS3Service.downloadInputFile("test",
					sampleImageBeforeUpload.getName());
			CopyUtils.copy(s3Object.getObjectContent(), sampleImageAfterUpload);
			Assert.assertTrue(sampleImageAfterUpload.length() > 0);
			Assert.assertEquals(sampleImageBeforeUpload.length(),
					sampleImageAfterUpload.length());
		}
		finally {
			Arrays.asList(sampleImageAfterUpload, sampleImageBeforeUpload)
					.forEach(File::delete);
		}
	}

	private static AmazonS3 amazonS3(String accessKey, String secret, String region) {
		var credentials = new BasicAWSCredentials(accessKey, secret);
		var timeout = 5 * 60 * 1000;
		var clientConfiguration = new ClientConfiguration()
				.withClientExecutionTimeout(timeout).withConnectionMaxIdleMillis(timeout)
				.withConnectionTimeout(timeout).withConnectionTTL(timeout)
				.withRequestTimeout(timeout);

		return AmazonS3ClientBuilder.standard()
				.withClientConfiguration(clientConfiguration)
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withRegion(Regions.fromName(region)).build();
	}

}