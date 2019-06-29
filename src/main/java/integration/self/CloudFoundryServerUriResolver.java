package integration.self;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Log4j2
class CloudFoundryServerUriResolver implements ServerUriResolver {

	private final ObjectMapper objectMapper;
	private final TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>() {
	};
	private final String applicationUrisKey = "application_uris";
	private final URI uri;

	CloudFoundryServerUriResolver(ObjectMapper om, String json) {
		this(om, json, strings -> strings.get(0));
	}

	@SneakyThrows
	CloudFoundryServerUriResolver(ObjectMapper om, String json, Function<List<String>, String> whichUriToSelect) {
		this.objectMapper = om;
		Map<String, Object> map = this.objectMapper.readValue(json, this.typeReference);
		Assert.notNull(map, "the result of reading the JSON must be non-null");
		Assert.isTrue(map.containsKey(this.applicationUrisKey), "the VCAP_SERVICES environment variable does not contain any routes");
		var urisRootObject = map.get(this.applicationUrisKey);
		Assert.isTrue(urisRootObject instanceof Collection, "the attribute must exist and it must be a " + Collection.class.getName());
		var selection = whichUriToSelect.apply(((List<String>) urisRootObject));
		this.uri = URI.create(selection);
	}

	@Override
	public URI resolveCurrentRootUri()   {
		return this.uri;
	}

}
