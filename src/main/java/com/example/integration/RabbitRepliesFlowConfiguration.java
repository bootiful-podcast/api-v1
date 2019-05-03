package com.example.integration;

import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

/**
 * Processed replies from the Python processor that need to be synced to Dropbox.
 */
@Log4j2
@Configuration
class RabbitRepliesFlowConfiguration {

	private final PodcastIntegrationProperties properties;

	RabbitRepliesFlowConfiguration(RabbitHelper helper,
			PodcastIntegrationProperties properties) {
		this.properties = properties;
		helper.defineDestination(properties.getRepliesExchange(),
				properties.getRepliesQueue(), properties.getRepliesRoutingKey());
	}

	@Bean
	IntegrationFlow rabbitRepliesFlow(ConnectionFactory cf) {
		var amqp = Amqp.inboundAdapter(cf, this.properties.getRepliesQueue()).get();
		return IntegrationFlows//
				.from(amqp)//
				.handle(new LoggingHandler())//
				.handle((o, messageHeaders) -> null) // this stops the flow
				.get();
	}

}
