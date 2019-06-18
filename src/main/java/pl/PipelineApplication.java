package pl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(PipelineProperties.class)
public class PipelineApplication {

	public static void main(String[] args) {
		SpringApplication.run(PipelineApplication.class, args);
	}

	@Bean
	PipelineService pipelineService(ChannelsConfiguration channels) {
		return new PipelineService(channels.apiToPipelineChannel());
	}

}
