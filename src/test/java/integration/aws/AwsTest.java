package integration.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import integration.PipelineProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;

public class AwsTest {

	private static AwsS3Service awsS3Service(PipelineProperties properties, AmazonS3 s3) {
		var s3Properties = properties.getS3();
		return new AwsS3Service(s3Properties.getInputBucketName(),
				s3Properties.getOutputBucketName(), s3);
	}

	private static AmazonS3 amazonS3(@Value("${AWS_ACCESS_KEY_ID}") String accessKey,
			@Value("${AWS_SECRET_ACCESS_KEY}") String secret,
			@Value("${AWS_REGION}") String region) {
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

	@Test
	public void download() throws Exception {
		var amazonS3 = amazonS3(System.getenv("AWS_ACCESS_KEY_ID"),
				System.getenv("AWS_SECRET_ACCESS_KEY"), System.getenv("AWS_REGION"));
		var amazonS3Service = new AwsS3Service("podcast-input-bucket",
				"podcast-output-bucket", amazonS3);
		var uid = "bd471b8c-fd55-4848-9184-91fca1630a41";
		var s3Object = amazonS3Service.downloadInputFile(uid, "dave-and-i.jpg");
		var file = new File(new File(System.getProperty("user.home"), "Desktop"),
				"file.jpg");
		try (var fin = s3Object.getObjectContent();
				var fout = new FileOutputStream(file)) {
			FileCopyUtils.copy(fin, fout);
		}
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

}