package integration.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitHelper {

	private final AmqpAdmin amqpAdmin;

	public void defineDestination(String exchange, String queue, String routingKey) {

		Queue q = this.queue(queue);
		amqpAdmin.declareQueue(q);

		Exchange e = this.exchange(exchange);
		amqpAdmin.declareExchange(e);

		Binding b = this.binding(q, e, routingKey);
		amqpAdmin.declareBinding(b);
	}

	public Exchange exchange(String requestExchange) {
		return ExchangeBuilder.topicExchange(requestExchange).durable(true).build();
	}

	public Queue queue(String requestsQueue) {
		return QueueBuilder.durable(requestsQueue).build();
	}

	public Binding binding(Queue q, Exchange e, String rk) {
		return BindingBuilder.bind(q).to(e).with(rk).noargs();
	}

}
