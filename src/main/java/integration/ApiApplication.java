package integration;

import integration.aws.AwsS3Service;
import integration.database.PodcastRepository;
import integration.self.ServerUriResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableConfigurationProperties(PipelineProperties.class)
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

	@Bean
	RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.build();
	}

	@Bean
	PipelineService pipelineService(PodcastRepository repository, AwsS3Service s3, ServerUriResolver resolver,
			Step1UnproducedPipelineIntegrationConfiguration left, Step1PreproducedIntegrationConfiguration right) {
		return new PipelineService(left.unproducedPipelineMessageChannel(), right.preproducedPipelineMessageChannel(),
				s3, repository, resolver);
	}

}
