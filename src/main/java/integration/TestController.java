package integration;

import integration.utils.CopyUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URI;

@Log4j2
@Deprecated
@RestController
class TestController {

	private final File file;

	TestController(@Value("file://${user.home}/Desktop/uploads") File uploads) {
		this.file = uploads;
		CopyUtils.ensureDirectoryExists(this.file);
		Assert.isTrue(this.file.exists(), () -> "the file " + this.file.getAbsolutePath() + " does not exist");

	}

	@PostMapping("/test-upload/{uid}")
	ResponseEntity<?> beginProduction(@PathVariable("uid") String uid, @RequestParam("file") MultipartFile file)
		throws Exception {
		var newFile = new File(this.file, uid + ".zip");
		log.info("going to upload (" + uid + ") a new file (" + file.getOriginalFilename() + ") to " + newFile.getAbsolutePath() + "");
		file.transferTo(newFile);
		CopyUtils.assertFileExists(newFile);
		log.info("the newly POST'd file lives at " + newFile.getAbsolutePath() + '.');
		var location = URI.create("/podcasts/" + uid + "/status");
		log.info("sending status location as : '" + location + "'");
		return ResponseEntity.accepted().location(location).build();
	}

}
