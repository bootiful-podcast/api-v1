package api;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import java.io.File;
import java.util.UUID;

@Log4j2
// @SpringBootApplication
@EnableConfigurationProperties(PodcastIntegrationProperties.class)
public class ApiApplication {

	private final File staging;

	private final PackageProcessIntegrationChannels channels;

	private final GenericHandler<File> unzipHandler = new GenericHandler<>() {

		@Override
		public Object handle(File file, MessageHeaders messageHeaders) {
			var dest = new File(staging, UUID.randomUUID().toString());
			Unzipper.unzip(file, dest);
			log.info("unzipping " + file.getAbsolutePath() + " to "
					+ dest.getAbsolutePath());
			return MessageBuilder.withPayload(dest).build();
		}
	};

	ApiApplication(@Value("${podcast.uploads.staging}") File staging,
			PackageProcessIntegrationChannels channels) {
		this.staging = staging;// todo
		this.channels = channels;
		log.info(
				"the staging directory, where uploaded files will be stored and processed for upload, is "
						+ this.staging.getAbsolutePath());
		Assert.isTrue(this.staging.exists() || this.staging.mkdirs(),
				"the directory " + this.staging.getAbsolutePath() + " doesn't exist");
	}

	private final GenericHandler<File> s3Uploader = new GenericHandler<File>() {

		@Override
		public Object handle(File file, MessageHeaders messageHeaders) {
			return null;
		}
	};

	private final GenericHandler<File> metadataWritingHandler = new GenericHandler<File>() {

		@Override
		public Object handle(File file, MessageHeaders messageHeaders) {
			return null;
		}
	};

	private final GenericHandler<File> audioProcessLaunchHandler = new GenericHandler<File>() {

		@Override
		public Object handle(File file, MessageHeaders messageHeaders) {
			return null;
		}
	};

	private final GenericHandler<File> podcastPublishHandler = new GenericHandler<File>() {

		@Override
		public Object handle(File file, MessageHeaders messageHeaders) {
			return null;
		}
	};

	@Bean
	IntegrationFlow pipelineFlow(AmqpTemplate template,
			PodcastIntegrationProperties properties) {

		var processor = properties.getProcessor();

		var audioProcessLaunchAmqpOutboundAdapter = Amqp.outboundAdapter(template)
				.exchangeName(processor.getRequestsExchange())
				.routingKey(processor.getRequestsRoutingKey());

		return IntegrationFlows //
				.from(this.channels.productionChannel()) //
				.handle(File.class, this.unzipHandler) // TODO this should be a splitter
														// that returns each file to
														// upload...
				/*
				 * .handle(File.class, this.s3Uploader) .handle(File.class,
				 * this.metadataWritingHandler)
				 * .handle(audioProcessLaunchAmqpOutboundAdapter) //
				 */
				.get();
	}

	@Bean
	MessageChannel rabbitRequestsChannel() {
		return MessageChannels.direct().get();
	}

	/*
	 * @SneakyThrows private String toJson(ProductionRequest request) { return
	 * this.objectMapper.writeValueAsString(request); }
	 */
	/*
	 * @Bean IntegrationFlow rabbitRequestsFlow(AmqpTemplate template) {
	 *
	 * var message = "the request must be a valid " +
	 * ProductionRequest.class.getSimpleName(); var amqpOutboundAdapter = Amqp //
	 * .outboundAdapter(template) // .exchangeName(this.properties.getRequestsExchange())
	 * // .routingKey(this.properties.getRequestsRoutingKey());
	 *
	 * return IntegrationFlows // .from(this.rabbitRequestsChannel()) // .handle((o,
	 * messageHeaders) -> { Assert.isTrue(o instanceof ProductionRequest, message); return
	 * o; }) // .transform(this::toJson) // .handle(amqpOutboundAdapter) // .get(); }
	 *
	 * @Bean MessageChannel rabbitRequestsChannel() { return
	 * MessageChannels.direct().get(); }
	 *
	 * @SneakyThrows private String toJson( ProductionRequest request) { return
	 * this.objectMapper.writeValueAsString(request); }
	 */

}
