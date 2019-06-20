package integration.rabbitmq;

import integration.PipelineProperties;
import org.springframework.stereotype.Component;

@Component
class RabbitInfrastructureInitializer {

	RabbitInfrastructureInitializer(
		PipelineProperties properties ,
		RabbitHelper helper) {

		var requestsExchange = properties
			.getProcessor()
			.getRequestsExchange();
		var requestsKey = properties
			.getProcessor()
			.getRequestsRoutingKey();
		var requestsQ = properties.getProcessor().getRequestsQueue();

		helper.

	}

}
