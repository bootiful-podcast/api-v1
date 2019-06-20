package pl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.UUID;

// todo remove this
@Deprecated
@Log4j2
@Configuration
@RequiredArgsConstructor
class Demo {

	private final RestTemplate template;

	private File file = new File("/Users/joshlong/Desktop/pkg.zip".trim());

	private <T> ResponseEntity<T> post(String url, File file, Class<T> replyClazz) {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		var resource = new FileSystemResource(file);
		var body = new LinkedMultiValueMap<>();
		body.add("file", resource);
		var requestEntity = new HttpEntity<>(body, headers);
		return template.postForEntity(url, requestEntity, replyClazz);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void postArchiveToService() {
		ResponseEntity<String> post = post(
				"http://localhost:8080/production?uid=" + UUID.randomUUID().toString(),
				file, String.class);
		Assert.isTrue(post.getStatusCode().is2xxSuccessful(),
				"the post to the production endpoint should've resulted in a success value");
	}

}
