package integration.self;

import java.net.URI;

/**
	* Strategy interface to look up the current running service's URI
	*/
public interface ServerUriResolver {

	URI resolveCurrentRootUri() throws Exception;
}
