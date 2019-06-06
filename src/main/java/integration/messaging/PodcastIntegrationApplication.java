package integration.messaging;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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

	public static void main(String[] args) {
		SpringApplication.run(PodcastIntegrationApplication.class, args);
	}

}
