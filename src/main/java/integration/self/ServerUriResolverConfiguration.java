package integration.self;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
class ServerUriResolverConfiguration {

	@Bean
	@Profile("default")
	LocalhostServerUriResolver localhostServerUriResolver() {
		return new LocalhostServerUriResolver();
	}

	@Bean
	@Profile("cloud")
	CloudFoundryServerUriResolver cloudFoundryServerUriResolver(ObjectMapper om) {
		return new CloudFoundryServerUriResolver(om, System.getenv("VCAP_APPLICATION"));
	}
}
