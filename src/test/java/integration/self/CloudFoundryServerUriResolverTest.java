package integration.self;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.stream.Collectors;

class CloudFoundryServerUriResolverTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void cloudfoundryWithMultipleRoutes() throws Exception {
		var json = "\t {\n" +
			"\t\t\t\"application_id\": \"c4311a79-1508-445d-befe-bb4ecc92df04\",\n" +
			"\t\t\t\t\"application_name\": \"rsb-site\",\n" +
			"\t\t\t\t\"application_uris\": [\n" +
			"\t\t\t\"rsb-site.cfapps.io\",\n" +
			"\t\t\t\t\"www.reactivespring.io\"\n" +
			"  ],\n" +
			"\t\t\t\"application_version\": \"667637e6-0b37-4af5-8c1e-0bb52dab66b4\",\n" +
			"\t\t\t\t\"cf_api\": \"https://api.run.pivotal.io\",\n" +
			"\t\t\t\t\"limits\": {\n" +
			"\t\t\t\t\"disk\": 512,\n" +
			"\t\t\t\t\t\"fds\": 16384,\n" +
			"\t\t\t\t\t\"mem\": 512\n" +
			"\t\t\t},\n" +
			"\t\t\t\"name\": \"rsb-site\",\n" +
			"\t\t\t\t\"space_id\": \"0b2ff245-c04f-4434-bc60-32e024042563\",\n" +
			"\t\t\t\t\"space_name\": \"reactive-spring-book\",\n" +
			"\t\t\t\t\"uris\": [\n" +
			"\t\t\t\"rsb-site.cfapps.io\",\n" +
			"\t\t\t\t\"www.reactivespring.io\"\n" +
			"  ],\n" +
			"\t\t\t\"users\": null,\n" +
			"\t\t\t\t\"version\": \"667637e6-0b37-4af5-8c1e-0bb52dab66b4\"\n" +
			"\t\t}\n";
		var cf = new CloudFoundryServerUriResolver(
			this.objectMapper, json, strings -> {
			var notCloudFoundryApps = strings.stream().filter(route -> !route.contains("cfapps"));
			var collected = notCloudFoundryApps.collect(Collectors.toList());
			return collected.get(0);
		}
		);
		URI uri = cf.resolveCurrentRootUri();
		Assert.assertNotNull(uri);
	}

	@Test
	void cloudfoundryWithOneRoute() throws Exception {
		var json = "\t {\n" +
			"\t\t\t\"application_id\": \"c4311a79-1508-445d-befe-bb4ecc92df04\",\n" +
			"\t\t\t\t\"application_name\": \"rsb-site\",\n" +
			"\t\t\t\t\"application_uris\": [\n" +
			"\t\t\t\t\"www.reactivespring.io\"\n" +
			"  ],\n" +
			"\t\t\t\"application_version\": \"667637e6-0b37-4af5-8c1e-0bb52dab66b4\",\n" +
			"\t\t\t\t\"cf_api\": \"https://api.run.pivotal.io\",\n" +
			"\t\t\t\t\"limits\": {\n" +
			"\t\t\t\t\"disk\": 512,\n" +
			"\t\t\t\t\t\"fds\": 16384,\n" +
			"\t\t\t\t\t\"mem\": 512\n" +
			"\t\t\t},\n" +
			"\t\t\t\"name\": \"rsb-site\",\n" +
			"\t\t\t\t\"space_id\": \"0b2ff245-c04f-4434-bc60-32e024042563\",\n" +
			"\t\t\t\t\"space_name\": \"reactive-spring-book\",\n" +
			"\t\t\t\t\"uris\": [\n" +
			"\t\t\t\"rsb-site.cfapps.io\",\n" +
			"\t\t\t\t\"www.reactivespring.io\"\n" +
			"  ],\n" +
			"\t\t\t\"users\": null,\n" +
			"\t\t\t\t\"version\": \"667637e6-0b37-4af5-8c1e-0bb52dab66b4\"\n" +
			"\t\t}\n" +
			"\t\t";
		var cf = new CloudFoundryServerUriResolver(this.objectMapper, json);
		URI uri = cf.resolveCurrentRootUri();
		Assert.assertNotNull(uri);
	}
}