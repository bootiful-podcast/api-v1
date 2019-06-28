package integration.self;

import org.springframework.stereotype.Component;

import java.net.URI;

// todo
@Component
class CloudFoundryServerUriResolver implements ServerUriResolver {

	@Override
	public URI resolveCurrentRootUri() throws Exception {
		return null;
	}

}
