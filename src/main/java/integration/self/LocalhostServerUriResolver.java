package integration.self;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

import java.net.InetAddress;
import java.net.URI;

@Log4j2
class LocalhostServerUriResolver extends AbstractServerUriResolver
		implements ServerUriResolver, ApplicationListener<WebServerInitializedEvent> {

	private int port;

	private String host;

	@Override
	@SneakyThrows
	public void onApplicationEvent(WebServerInitializedEvent event) {
		var localHost = InetAddress.getLocalHost();
		this.host = localHost.getHostName();
		this.port = event.getWebServer().getPort();
	}

	@Override
	public URI resolveCurrentRootUri() {
		return this.buildUriFor(this.host + ':' + this.port);
	}

}
