package api;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;

@Log4j2
@RestController
class PackageUploadController {

	private final File file;
	private final MessageChannel productionChannel;

	PackageUploadController(File staging,
																									MessageChannel channel) {
		this.file = staging;
		this.productionChannel = channel;
	}

	private String buildMessage(String id, MultipartFile file) {
		var details = new String[]{
			System.lineSeparator(),
			"ID: " + id,
			"original file name: " + file.getOriginalFilename(),
			"size: " + file.getSize(),
			"content-type: " + file.getContentType()
		};
		return StringUtils
			.arrayToDelimitedString(details, System.lineSeparator());
	}

	@PostMapping("/production")
	ResponseEntity<?> beginProduction(
		@RequestParam("id") String id,
		@RequestParam("file") MultipartFile file) throws Exception {
		log.info(this.buildMessage(id, file));
		var newFile = new File(this.file, id);
		file.transferTo(newFile);

		var msg = MessageBuilder
			.withPayload(newFile)
			.setHeader(PackageProcessHeaders.PACKAGE_ID_HEADER, id)
			.build();
		this.productionChannel.send(msg);

		return ResponseEntity
			.accepted()
			.body(Collections.singletonMap("status", "OK"));

	}


}
