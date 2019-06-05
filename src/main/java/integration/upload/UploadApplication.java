package integration.upload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.function.Consumer;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
class UploadApplication {

	private final Log log = LogFactory.getLog(getClass());

	public static void main(String[] args) {
		SpringApplication.run(UploadApplication.class, args);
	}

	@Bean
	WebClient client(WebClient.Builder builder) {
		return builder.build();
	}

	private final Consumer<Part> partConsumer = part -> part.content()
		.doOnNext(DataBufferUtils::release).subscribe();

	@Bean
	RouterFunction<ServerResponse> routes(WebClient client) {

		return route()//
			.POST("/t1", request -> request.body(BodyExtractors.toFlux(Part.class))
				.filter(part -> part.name().equals("file")).next()
				.flatMap(fp -> {
					Mono<ServerResponse> mono = client
						.post()
						.uri("http://localhost:8080/t2")
						.body(BodyInserters.fromMultipartData("file", fp))
						.retrieve().bodyToMono(Void.class)
						.then(ok().build());
					return mono;
				})
				.doOnDiscard(Part.class, partConsumer)) //
			.POST("/t2", request -> {
				Mono<ServerResponse> mono = request
					.body(BodyExtractors.toFlux(Part.class))
					.filter(part -> part.name().equals("file")).map(p -> (FilePart) p)
					.next().doOnDiscard(Part.class, partConsumer)
					.flatMap(filePart -> filePart
						.transferTo(new File("/Users/joshlong/Desktop/out.wav"))
						.then(ok().build()));
				return mono;
			})//
			.build();
	}
	// curl -F "file=@/Users/joshlong/Desktop/pipeline/input/oleg-interview.wav" http://localhost:8080/t1

}
