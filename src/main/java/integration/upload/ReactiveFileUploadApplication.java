package integration.upload;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Log4j2
@SpringBootApplication
public class ReactiveFileUploadApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveFileUploadApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routes(UploadHandler handler) {
		var uploads = "/uploads";
		var introductionPath = uploads + "/introductions";
		var interviewPath = uploads + "/interviews";
		var root = new File(System.getProperty("user.home"), "/Desktop");
		var pathMap = List
			.of(introductionPath, interviewPath)
			.stream()
			.collect(Collectors.toMap(x -> x, x -> {
				var parentFile = new File(root, x);
				log.info("parentFile : " + parentFile.getAbsolutePath());
				Assert.isTrue(parentFile.exists() || parentFile.mkdirs(), "the directory " + parentFile.getAbsolutePath() + " does not exist");
				return parentFile;
			}));
		var route = route();
		pathMap.forEach((k, f) -> route.POST(k + "/{id}", request -> handler.uploadTo(f, request)));
		return route.build();
	}
}

@Log4j2
@Component
class UploadHandler {

	private final String fileAttributeName = "file";
	private final Scheduler scheduler = Schedulers.elastic();

	@SneakyThrows
	private static void ensureExists(File e) {
		Assert.isTrue(e.exists() && e.delete() || !e.exists(), "there should be no target file");
		FileUtils.touch(e);
	}

	// curl -F "file=@/Users/joshlong/Desktop/" localhost:8080/uploads/introductions/2232
	Mono<ServerResponse> uploadTo(File dest, ServerRequest request) {
		log.info("trying to upload to " + dest.getAbsolutePath());
		var id = request.pathVariable("id");
		return request
			.body(BodyExtractors.toMultipartData())
			.flatMap(parts -> {
					var partsMap = parts.toSingleValueMap();
					var fp = (FilePart) partsMap.get(this.fileAttributeName);
					var fnParts = this.partsForFileName(fp.filename());
					var uploadedFile = new File(dest, id + fnParts[1]);
					var uploadedPath = uploadedFile.toPath();
					ensureExists(uploadedFile);
					var file = uploadedPath.getParent().toFile();
					Assert.isTrue(file.exists() || file.mkdirs(), file.getAbsolutePath() + " does not exist");
					return fp
						.transferTo(uploadedPath.toAbsolutePath())
						.doOnSuccess(v -> log.info("wrote " + uploadedPath.toFile().getAbsolutePath() + " for incoming file " + fp.filename() +
							" having length " + uploadedFile.length() + " bytes"));
				}
			)
			.subscribeOn(this.scheduler)
			.then(ServerResponse.ok().build());
	}

	private String[] partsForFileName(String fileName) {
		log.info("new file name: " + fileName);
		var liext = fileName.lastIndexOf(".");
		var ext = "";
		if (liext != -1) {
			ext = fileName.substring(liext);
			if (!ext.startsWith(".")) {
				ext = "." + ext;
				fileName = fileName.substring(0, liext);
			}
		}
		return new String[]{fileName, ext};
	}

}