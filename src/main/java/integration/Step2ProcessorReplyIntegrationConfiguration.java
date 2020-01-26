package integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sendgrid.helpers.mail.objects.Email;
import fm.bootifulpodcast.rabbitmq.RabbitMqHelper;
import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.email.NotificationService;
import integration.events.PodcastProcessedEvent;
import integration.utils.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * This is step 2 in the flow.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Log4j2
@RequiredArgsConstructor
@Configuration
class Step2ProcessorReplyIntegrationConfiguration {

	private final ApplicationEventPublisher publisher;

	@Bean
	IntegrationFlow processorReplyPipeline(PipelineProperties properties, RabbitMqHelper helper, AmqpTemplate template,
			PipelineService service, PodcastRepository repository, JsonHelper jsonService,
			ConnectionFactory connectionFactory, NotificationService emailer) {

		var podbeanConfiguration = properties.getPodbean();
		helper.defineDestination(podbeanConfiguration.getRequestsExchange(), podbeanConfiguration.getRequestsQueue(),
				podbeanConfiguration.getRequestsRoutingKey());
		var processorOutboundAdapter = Amqp//
				.outboundAdapter(template)//
				.exchangeName(podbeanConfiguration.getRequestsExchange()) //
				.routingKey(podbeanConfiguration.getRequestsRoutingKey());
		var repliesQueue = properties.getProcessor().getRepliesQueue();
		var amqpInboundAdapter = Amqp //
				.inboundAdapter(connectionFactory, repliesQueue) //
				.get();
		var outputFileKey = "output-file";
		return IntegrationFlows //
				.from(amqpInboundAdapter) //
				.handle(String.class, (payload, headers) -> {
					var reference = new TypeReference<Map<String, String>>() {
					};
					var resultMap = jsonService.fromJson(payload, reference);
					var outputFile = resultMap.getOrDefault("mp3", resultMap.getOrDefault("wav", null));
					var uid = resultMap.get("uid");
					resultMap.put(outputFileKey, outputFile);
					var outputBucketName = resultMap.get("output-bucket-name");
					this.recordProcessedFilesToDatabase(uid, outputBucketName, outputFile);
					return resultMap;
				}) //
				.handle((GenericHandler<Map<String, String>>) (payload, headers) -> {
					var uid = payload.get("uid");
					return repository.findByUid(uid).map(podcast -> {
						var data = Map.<String, Object>of(//
								"uid", uid, //
								"title", podcast.getTitle(), //
								"outputMediaUri", service.buildMediaUriForPodcastById(podcast.getId()).toString());
						log.info("sending the following data into the template: " + data);
						var fileUploadedTemplate = "file-uploaded.ftl";
						var content = emailer.render(fileUploadedTemplate, data);
						var notificationsProperties = properties.getNotifications();
						var response = emailer.send(new Email(notificationsProperties.getToEmail()),
								new Email(notificationsProperties.getFromEmail()), notificationsProperties.getSubject(),
								content);
						var xxSuccessful = HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful();
						Assert.isTrue(xxSuccessful,
								"tried to send a notification email with SendGrid, and got back a non-positive status code. "
										+ response.getBody());
						return podcast;
					}).orElse(null);
				})//
				.transform(Podcast::getUid)//
				.handle(processorOutboundAdapter)//
				.get();//
	}

	private void recordProcessedFilesToDatabase(String uid, String outputBucketName, String fn) {
		this.publisher.publishEvent(new PodcastProcessedEvent(uid, outputBucketName, fn));
	}

}
