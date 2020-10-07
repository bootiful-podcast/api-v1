package integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sendgrid.helpers.mail.objects.Email;
import fm.bootifulpodcast.rabbitmq.RabbitMqHelper;
import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.email.NotificationService;
import integration.events.PodcastProcessedEvent;
import integration.utils.JsonHelper;
import integration.utils.PipelineUtils;
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
import org.springframework.util.Assert;

import java.util.Map;

/**
 * This is step 2 in the flow.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Log4j2
@Configuration
@RequiredArgsConstructor
class Step2ProcessorReplyIntegrationConfiguration {

	private final ApplicationEventPublisher publisher;

	private final PipelineProperties properties;

	private final RabbitMqHelper helper;

	private final AmqpTemplate template;

	private final PipelineService service;

	private final PodcastRepository repository;

	private final JsonHelper jsonService;

	private final ConnectionFactory connectionFactory;

	private final NotificationService emailer;

	private final TypeReference<Map<String, String>> reference = new TypeReference<Map<String, String>>() {
	};

	@Bean
	IntegrationFlow processorReplyPipeline() {

		var podbeanConfiguration = properties.getPodbean();
		helper.defineDestination(podbeanConfiguration.getRequestsExchange(), podbeanConfiguration.getRequestsQueue(),
				podbeanConfiguration.getRequestsRoutingKey());
		return IntegrationFlows //
				.from(Amqp //
						.inboundAdapter(this.connectionFactory, this.properties.getProcessor().getRepliesQueue()) //
						.get()//
				) //
				.handle(String.class, (payload, headers) -> {
					var map = this.jsonService.fromJson(payload, reference);
					return handleReply(map.get("uid"), map.get("mp3"), map.get("output-bucket-name"));
				})//
				.transform(Podcast::getUid)//
				.handle(Amqp//
						.outboundAdapter(template)//
						.exchangeName(podbeanConfiguration.getRequestsExchange()) //
						.routingKey(podbeanConfiguration.getRequestsRoutingKey()))//
				.get();//
	}

	private Podcast handleReply(String uid, String outputFileName, String outputBucketName) {
		var notificationsProperties = this.properties.getNotifications();

		this.publisher.publishEvent(new PodcastProcessedEvent(uid, outputBucketName, outputFileName));

		return PipelineUtils.podcastOrElseThrow(uid, this.repository.findByUid(uid).map(podcast -> {
			var data = Map.<String, Object>of(//
					"uid", uid, //
					"title", podcast.getTitle(), //
					"outputMediaUri", this.service.buildMediaUriForPodcastById(podcast.getId()).toString());
			var content = this.emailer.render("file-uploaded.ftl", data);
			var response = this.emailer.send(new Email(notificationsProperties.getToEmail()),
					new Email(notificationsProperties.getFromEmail()), notificationsProperties.getSubject(), content);
			Assert.isTrue(HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful(),
					"tried to send a notification email with SendGrid, and got back a non-positive status code. "
							+ response.getBody());
			return podcast;
		}));

	}

}
