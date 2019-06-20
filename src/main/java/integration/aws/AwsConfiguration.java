package integration.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import integration.PipelineProperties;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
class AwsConfiguration {

	@Bean
	AwsS3Service awsS3Service(PipelineProperties properties, AmazonS3 s3) {
		return new AwsS3Service(properties.getS3().getBucketName(), s3);
	}

	@Bean
	AmazonS3 amazonS3(@Value("${aws_access_key_id}") String accessKey,
			@Value("${aws_secret_access_key}") String secret,
			@Value("${aws_region}") String region) {

		log.info(accessKey);
		log.info(secret);
		log.info(region);
		var credentials = new BasicAWSCredentials(accessKey, secret);
		return AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withRegion(Regions.fromName(region)).build();
	}

}
