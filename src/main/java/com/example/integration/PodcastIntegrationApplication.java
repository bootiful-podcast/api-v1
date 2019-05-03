package com.example.integration;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * this will establish a monitor for Dropbox and publish an event whenever a new file
 * arrives. The new event will be in turn sent to our Python code to turn the {@code .wav}
 * files into a new podcast episode. We'll get a response from the Python code that will
 * come in on a different queue and we'll use that as the indication that we should be
 * uploading the resulting file to Dropbox and Soundcloud.
 *
 * @author Josh Long
 */
@Log4j2
@SpringBootApplication
@EnableConfigurationProperties(PodcastIntegrationProperties.class)
public class PodcastIntegrationApplication {

	PodcastIntegrationApplication(RabbitProperties rabbitProperties) {
		log.info("RabbitMQ address: " + rabbitProperties.getAddresses());
	}

	@Bean
	RouterFunction<ServerResponse> routes(RabbitRequestsFlowConfiguration requests) {

		var channel = requests.rabbitRequestsChannel();

		return route() //
				.POST("/start", request -> {
					var aClass = ProductionRequest.class;
					var productionRequestMono = request.bodyToMono(aClass).map(pr -> {
						var msg = MessageBuilder.withPayload(pr).build();
						channel.send(msg);
						return pr;
					});
					return ServerResponse.ok().body(productionRequestMono, aClass);
				}) //
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(PodcastIntegrationApplication.class, args);
	}

}
