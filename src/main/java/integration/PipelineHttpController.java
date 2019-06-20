package integration;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import integration.utils.FileUtils;

import java.io.File;
import java.util.Map;

@Log4j2
@RestController
class PipelineHttpController {

	private final File file;

	private final PipelineService service;

	PipelineHttpController(PipelineProperties props, PipelineService service) {
		this.file = FileUtils.ensureDirectoryExists(props.getS3().getStagingDirectory());
		this.service = service;
	}

	@PostMapping("/production")
	ResponseEntity<?> beginProduction(@RequestParam("uid") String uid,
			@RequestParam("file") MultipartFile file) throws Exception {
		var newFile = new File(this.file, uid);
		file.transferTo(newFile);
		FileUtils.assertFileExists(newFile);
		Assert.isTrue(this.service.launchPipeline(uid, newFile),
				"the pipeline says no.");
		return ResponseEntity.accepted().body(Map.of("status", "OK"));
	}

}
