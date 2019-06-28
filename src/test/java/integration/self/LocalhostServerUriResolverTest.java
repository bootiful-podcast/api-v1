package integration.self;

import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;

import java.net.InetAddress;
import java.net.URI;

@Log4j2
class LocalhostServerUriResolverTest {

	private final LocalhostServerUriResolver serverUriResolver = new LocalhostServerUriResolver();

	@Test
	void resolve() throws Exception {

		var port = 8080;
		var mock = Mockito.mock(WebServerApplicationContext.class);
		var mockWs = Mockito.mock(WebServer.class);
		Mockito.when(mock.getWebServer()).thenReturn(mockWs);
		Mockito.when(mockWs.getPort()).thenReturn(port);

		var event = new WebServerInitializedEvent(mockWs) {
			@Override
			public WebServerApplicationContext getApplicationContext() {
				return mock;
			}
		};
		this.serverUriResolver.onApplicationEvent(event);
		URI uri = this.serverUriResolver.resolveCurrentRootUri();
		log.info("uri: " + uri);
		var hostName = InetAddress.getLocalHost().getHostName();
		Assert.assertTrue(uri.toString().contains(hostName + ":" + port));
	}

}