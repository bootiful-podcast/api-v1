package integration.aws;

import integration.utils.CopyUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(initializers = { AwsS3ServiceTest.Initializer.class })
public class AwsS3ServiceTest {

	@ClassRule
	public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:11.1")//
			.withDatabaseName("integration-tests-db") //
			.withUsername("sa")//
			.withPassword("sa");

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			TestPropertyValues
					.of("spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
							"spring.datasource.username=" + postgreSQLContainer.getUsername(),
							"spring.datasource.password=" + postgreSQLContainer.getPassword())
					.applyTo(configurableApplicationContext.getEnvironment());
		}

	}

	private final Resource resource = new ClassPathResource("/sample-image.jpg");

	@Autowired
	private AwsS3Service amazonS3Service;

	@Test
	public void uploadAndDownload() throws Exception {
		var sampleImageBeforeUpload = File.createTempFile("sample-image-before-upload", ".jpg");
		var sampleImageAfterUpload = File.createTempFile("sample-image-after-upload", ".jpg");
		try {
			CopyUtils.copy(this.resource.getInputStream(), sampleImageBeforeUpload);
			this.amazonS3Service.uploadInputFile(MediaType.IMAGE_JPEG_VALUE, "test", sampleImageBeforeUpload);
			var s3Object = this.amazonS3Service.downloadInputFile("test", sampleImageBeforeUpload.getName());
			CopyUtils.copy(s3Object.getObjectContent(), sampleImageAfterUpload);
			Assert.assertTrue(sampleImageAfterUpload.length() > 0);
			Assert.assertEquals(sampleImageBeforeUpload.length(), sampleImageAfterUpload.length());
		}
		finally {
			Arrays.asList(sampleImageAfterUpload, sampleImageBeforeUpload).forEach(File::delete);
		}
	}

}