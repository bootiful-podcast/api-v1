package pl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class Json {

	private final ObjectMapper objectMapper;

	@SneakyThrows
	public String toJson(Object o) {
		return this.objectMapper.writeValueAsString(o);
	}

	@SneakyThrows
	public <T> T fromJson(String json, Class<T> clazz) {
		return this.objectMapper.readValue(json, clazz);
	}

	@SneakyThrows
	public <T> T fromJson(String json, TypeReference<T> clazz) {
		return this.objectMapper.readValue(json, clazz);
	}

}
