package integration.upload;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;

@SpringBootApplication
public class HttpUploadApplication {

	public static void main(String[] args) {
		SpringApplication.run(HttpUploadApplication.class, args);
	}
}

@Log4j2
@RestController
class HttpUploadRestController {

	private final File file;

	HttpUploadRestController(@Value("${user.home}") File home) {
		this.file = new File(home, "Desktop/output");
		Assert.isTrue(this.file.exists() || this.file.mkdirs(), "the directory " + this.file.getAbsolutePath() + " doesn't exist");
		log.info("output directory is " + this.file.getAbsolutePath());
	}

	@PostMapping("/production")
	ResponseEntity<?> beginProduction(@RequestParam("file") MultipartFile file,
																																			@RequestParam("id") String id) throws Exception {
		log.info("ID: " + id);
		log.info("original file name: " + file.getOriginalFilename());
		log.info("size: " + file.getSize());
		log.info("content-type: " + file.getContentType());
		var newFile = new File(this.file, id);
		file.transferTo(newFile);
		return ResponseEntity.ok().body(Collections.singletonMap("status", "OK"));
	}
}


