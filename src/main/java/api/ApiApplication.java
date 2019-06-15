package api;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

@Log4j2
@EnableAutoConfiguration
@Configuration
@Import(PackageProcessIntegrationChannels.class)
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

	private final File staging;

	ApiApplication(@Value("${podcast.uploads.staging}") File staging) {
		this.staging = staging;
		log.info("the staging directory, where uploaded files will be stored and processed for upload, is " + this.staging.getAbsolutePath());
		Assert.isTrue(this.staging.exists() || this.staging.mkdirs(), "the directory " + this.staging.getAbsolutePath() + " doesn't exist");
	}

	private final GenericHandler<File> unzipHandler = new GenericHandler<>() {

		@Override
		public Object handle(File file, MessageHeaders messageHeaders) {
			var dest = new File(staging, UUID.randomUUID().toString());
			Unzipper.unzip(file, dest);
			log.info("unzipping " + file.getAbsolutePath() + " to " + dest.getAbsolutePath());
			return MessageBuilder
				.withPayload(dest)
				.build();
		}
	};

	@Bean
	IntegrationFlow integrationFlow(PackageProcessIntegrationChannels channels) {
		return IntegrationFlows
			.from(channels.productionChannel())
			.handle(File.class, this.unzipHandler)
			.handle(File.class, (file, messageHeaders) -> {
				for (var f : Objects.requireNonNull(file.listFiles())) {
					log.info("file: " + f.getAbsolutePath());
				}
				return null;
			})
			.get();
	}

	@Bean
	PackageUploadController packageUploadController(
		PackageProcessIntegrationChannels channels) {
		return new PackageUploadController(this.staging, channels.productionChannel());
	}

	/*
	@Bean
	ApplicationRunner run() {
		return args -> {

		};
	}
	*/
}
