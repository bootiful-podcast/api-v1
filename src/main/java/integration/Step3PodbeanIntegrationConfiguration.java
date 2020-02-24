package integration;

import fm.bootifulpodcast.podbean.EpisodeStatus;
import fm.bootifulpodcast.podbean.EpisodeType;
import fm.bootifulpodcast.podbean.PodbeanClient;
import integration.aws.AwsS3Service;
import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.events.PodcastPublishedToPodbeanEvent;
import integration.utils.CopyUtils;
import integration.utils.PipelineUtils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Optional;
import java.util.function.Function;

/**
 * This is step 3 in the flow.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Log4j2
@Configuration
class Step3PodbeanIntegrationConfiguration {

	@Bean
	IntegrationFlow podbeanPublicationPipeline(ApplicationEventPublisher publisher, AwsS3Service s3Service,
			AmqpTemplate template, ConnectionFactory connectionFactory, PodcastRepository repository,
			PodbeanClient podbeanClient, PipelineProperties pipelineProperties) {

		var siteGeneratorRequests = Amqp//
				.outboundAdapter(template)//
				.exchangeName(pipelineProperties.getSiteGenerator().getRequestsExchange()) //
				.routingKey(pipelineProperties.getSiteGenerator().getRequestsRoutingKey());

		var amqpInboundAdapter = Amqp //
				.inboundAdapter(connectionFactory, pipelineProperties.getPodbean().getRequestsQueue()) //
				.get();

		var podbeanDirectory = pipelineProperties.getPodbean().getPodbeanDirectory();
		CopyUtils.ensureDirectoryExists(podbeanDirectory);
		return IntegrationFlows//
				.from(amqpInboundAdapter)//
				.handle(String.class, (str, messageHeaders) -> {
					log.info("the incoming value is " + str);
					var r = repository.findByUid(str).orElse(null);
					if (null == r) {
						var msg = PipelineUtils.podcastNotFoundErrorMessage(str);
						log.warn(msg);
					}
					return r;
				})
				// .transform(incoming -> repository.findByUid((String) incoming).get())
				.handle((GenericHandler<Podcast>) (podcast, messageHeaders) -> {
					var mp3FileName = fileNameFor(podcast, "mp3");
					var mp3File = new File(podbeanDirectory, mp3FileName);
					this.downloadFromS3(s3Service, podcast, mp3File, mp3FileName);
					var mp3Upload = podbeanClient.upload(MediaType.parseMediaType("audio/mpeg"), mp3File,
							mp3File.length());
					var jpgFileName = fileNameFor(podcast, "jpg");
					var jpgFile = new File(podbeanDirectory, jpgFileName);
					this.downloadFromS3(s3Service, podcast, jpgFile, jpgFileName);
					var jpgUpload = podbeanClient.upload(MediaType.IMAGE_JPEG, jpgFile, jpgFile.length());
					var episode = podbeanClient.publishEpisode(podcast.getTitle(), podcast.getDescription(),
							EpisodeStatus.PUBLISH, EpisodeType.PUBLIC, mp3Upload.getFileKey(), jpgUpload.getFileKey());
					publisher.publishEvent(new PodcastPublishedToPodbeanEvent(podcast.getUid(), episode.getMediaUrl(),
							episode.getPlayerUrl(), episode.getLogoUrl()));
					log.info("the episode has been published to " + episode.toString() + '.');
					Assert.isTrue(mp3File.exists() && mp3File.delete(),
							"the" + " file " + mp3File.getAbsolutePath() + " does not exist or could not be deleted");
					return true;
				})//
				.handle(siteGeneratorRequests)//
				.get();
	}

	@SneakyThrows
	private void downloadFromS3(AwsS3Service s3Service, Podcast podcast, File file, String fn) {
		var s3Object = s3Service.downloadOutputFile(podcast.getUid(), fn);
		FileCopyUtils.copy(s3Object.getObjectContent(), new FileOutputStream(file));
		Assert.isTrue(file.exists() && file.length() > 0,
				"the file could not be downloaded to " + file.getAbsolutePath() + ".");
	}

	private static String fileNameFor(Podcast podcast, String ext) {
		return podcast.getUid() + "." + (ext.toLowerCase());
	}

	@Bean
	MessageChannel podbeanPublicationChannel() {
		return MessageChannels.direct().get();
	}

}
