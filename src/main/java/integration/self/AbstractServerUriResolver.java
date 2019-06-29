package integration.self;

import org.springframework.util.Assert;

import java.net.URI;

abstract class AbstractServerUriResolver implements ServerUriResolver {

	protected URI buildUriFor(String host) {

		Assert.notNull(host, "the host can't be null");

		var suffix = "/";

		if (!host.endsWith(suffix)) {
			host = host + suffix;
		}

		if (!host.toLowerCase().startsWith("http")) {
			host = "http://" + host;
		}

		return URI.create(host);
	}

}
