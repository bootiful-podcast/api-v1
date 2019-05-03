package com.example.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

@Configuration
@RequiredArgsConstructor
class RabbitRequestsFlowConfiguration {

	private final ObjectMapper objectMapper;

	private final PodcastIntegrationProperties properties;

	@Bean
	IntegrationFlow rabbitRequestsFlow(AmqpTemplate template) {

		var message = "the request must be a valid "
				+ ProductionRequest.class.getSimpleName();
		var amqpOutboundAdapter = Amqp //
				.outboundAdapter(template) //
				.exchangeName(this.properties.getRequestsExchange()) //
				.routingKey(this.properties.getRequestsRoutingKey());

		return IntegrationFlows //
				.from(this.rabbitRequestsChannel()) //
				.handle((o, messageHeaders) -> {
					Assert.isTrue(o instanceof ProductionRequest, message);
					return o;
				}).transform(this::toJson).handle(amqpOutboundAdapter).get();
	}

	@Bean
	MessageChannel rabbitRequestsChannel() {
		return MessageChannels.direct().get();
	}

	@SneakyThrows
	private String toJson(ProductionRequest request) {
		return this.objectMapper.writeValueAsString(request);
	}

}
