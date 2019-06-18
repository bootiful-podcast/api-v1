package pl;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

@Log4j2
@RestController
class PackageUploadController {

	private final File file;

	private final MessageChannel apiToPipelineChannel;

	PackageUploadController(PipelineProperties props, ChannelsConfiguration channels) {
		this.file = FileUtils.ensureDirectoryExists(props.getS3().getStagingDirectory());
		this.apiToPipelineChannel = channels.apiToPipelineChannel();
	}

	@PostMapping("/production")
	ResponseEntity<?> beginProduction(@RequestParam("id") String id,
			@RequestParam("file") MultipartFile file) throws Exception {
		var newFile = new File(this.file, id);
		file.transferTo(newFile);
		FileUtils.assertFileExists(newFile);
		var msg = MessageBuilder.withPayload(newFile).setHeader(Headers.PACKAGE_ID, id)
				.build();
		this.apiToPipelineChannel.send(msg);
		return ResponseEntity.accepted().body(Map.of("status", "OK"));
	}

}
