package integration;

import integration.events.PodcastArchiveUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

/**
 * This skips the the upload preparation and the Python-based processor and goes straight
 * to the reply flow that kicks in once a response comes back from the RabbitMQ processor.
 *
 * <p>
 * I want to publish the episodes into the new pipeline. But we need to take advantage of
 * only the part of the process after the message has returned from the Python processor.
 * <p>
 * The pre-requirements:
 * <OL>
 * <LI>The image and the file must be uploaded already to the output bucket on S3</LI>
 * <LI>A Podcast record must have been recorded in the DB. (see the various event handlers
 * triggered in Step1UploadPreparationIntegrationConfiguration)</LI>
 * <LI>a new message needs to arrive on the right message queue so as to trigger
 * Step2ProcessorReplyIntegrationConfiguration</LI>
 * </OL>
 */
// TODO
@Log4j2
@Configuration
@RequiredArgsConstructor
class Step1PreproducedIntegrationConfiguration {

	private final ApplicationEventPublisher publisher;

	/*
	 * @Bean IntegrationFlow fastTrackIntegrationFlow() { return IntegrationFlows //
	 * .from(this.preproducedPipelineMessageChannel())// .handle(ProducedPodcast.class,
	 * (producedPodcast, messageHeaders) -> {
	 *
	 * var uploadPackageManifest = PreproducedPodcastPackageManifest.from(
	 * producedPodcast.getUid(), producedPodcast.getTitle(),
	 * producedPodcast.getDescription(), producedPodcast.getProducedAudio().getName(),
	 * producedPodcast.getEpisodePhoto().getName()); publisher.publishEvent(new
	 * PodcastArchiveUploadedEvent(uploadPackageManifest));
	 *
	 * return null; }).get(); }
	 */

	@Bean
	MessageChannel preproducedPipelineMessageChannel() {
		return MessageChannels.direct().get();
	}

}
