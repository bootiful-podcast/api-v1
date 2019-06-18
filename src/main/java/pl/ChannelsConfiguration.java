package pl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;

@Configuration
class ChannelsConfiguration {

	@Bean
	MessageChannel apiToPipelineChannel() {
		return MessageChannels.direct().get();
	}

}
