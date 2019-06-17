package pl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Function;

@Log4j2
@Configuration
class AwsConfiguration {

	@SneakyThrows
	private static void readFileIntoEnvironment(File file, String psPrefix,
			AbstractEnvironment environment, Function<String, String> mapper) {

		FileUtils.assertFileExists(file);
		try (var reader = new FileReader(file)) {
			var propertyProperties = new Properties();
			propertyProperties.load(reader);

			var map = new HashMap<String, Object>();
			propertyProperties.keySet().forEach(oldKey -> {
				var newKey = mapper.apply((String) oldKey);
				map.put(newKey, propertyProperties.getProperty((String) oldKey).trim());
			});

			var mapPropertySource = new MapPropertySource(psPrefix, map);
			environment.getPropertySources().addLast(mapPropertySource);
		}
	}

	@SneakyThrows
	AwsConfiguration(AbstractEnvironment environment) {
		var awsRoot = new File(System.getProperty("user.home"), ".aws");
		readFileIntoEnvironment(new File(awsRoot, "credentials"), "aws-credentials",
				environment, (key) -> key);
		readFileIntoEnvironment(new File(awsRoot, "config"), "aws-config", environment,
				k -> {
					if (k.equalsIgnoreCase("region")) {
						return "aws_region";
					}
					return k;
				});
	}

	@Bean
	AwsS3Service awsS3Service(PipelineProperties properties, AmazonS3 s3) {
		return new AwsS3Service(properties.getS3().getBucketName(), s3);
	}

	@Bean
	AmazonS3 amazonS3(@Value("${aws_access_key_id}") String accessKey,
			@Value("${aws_secret_access_key}") String secret,
			@Value("${aws_region}") String region) {
		var credentials = new BasicAWSCredentials(accessKey, secret);
		return AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withRegion(Regions.fromName(region)).build();
	}

}
