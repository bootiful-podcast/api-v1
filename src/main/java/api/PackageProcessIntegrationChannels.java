package api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;

@Configuration
class PackageProcessIntegrationChannels {

	@Bean
	MessageChannel productionChannel() {
		return MessageChannels.direct().get();
	}
}
