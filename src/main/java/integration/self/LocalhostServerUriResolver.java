package integration.self;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;

@Component
@Log4j2
@Profile("default")
class LocalhostServerUriResolver
		implements ServerUriResolver, ApplicationListener<WebServerInitializedEvent> {

	private int port;

	private String host;

	@Override
	@SneakyThrows
	public void onApplicationEvent(WebServerInitializedEvent event) {
		int port = event.getWebServer().getPort();
		var localHost = InetAddress.getLocalHost();
		var host = localHost.getHostName();
		initialize(host, port);
	}

	private void initialize(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public URI resolveCurrentRootUri() throws Exception {
		return URI.create("http://" + this.host + ':' + this.port);
	}

}
