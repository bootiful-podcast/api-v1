package integration;

import com.amazonaws.services.s3.model.S3Object;
import integration.aws.AwsS3Service;
import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.utils.FileUtils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URI;
import java.util.Map;

@Log4j2
@RestController
class PipelineHttpController {

	private final File file;

	private final PipelineService service;

	private final AwsS3Service s3;

	private final PodcastRepository podcastRepository;

	private final String processingMessage = "processing";

	private final String finishedMessage = "finished processing";

	private final MediaType mediaContentType = MediaType
			.parseMediaType("binary/octet-stream");

	PipelineHttpController(PipelineProperties props, AwsS3Service s3,
			PodcastRepository repository, PipelineService service) {
		this.file = FileUtils.ensureDirectoryExists(props.getS3().getStagingDirectory());
		this.service = service;
		this.s3 = s3;
		this.podcastRepository = repository;
	}

	@GetMapping("/podcasts/{uid}/status")
	ResponseEntity<?> getStatusForPodcast(@PathVariable String uid) {
		var byUid = podcastRepository.findByUid(uid);
		var response = byUid.map(podcast -> {
			Map<String, String> statusMap;
			if (null != podcast.getMediaS3Uri()) {
				statusMap = Map.of( //
						"media-url", "/podcasts/" + podcast.getUid() + "/output", //
						"status", this.finishedMessage //
				);
			}
			else {
				statusMap = Map.of("status", this.processingMessage);
			}
			log.info("returning status: " + statusMap.toString() + " for " + uid);
			return statusMap;
		});
		return response.map(reply -> ResponseEntity.ok().body(reply))
				.orElse(ResponseEntity.noContent().build());
	}

	@SneakyThrows
	@GetMapping("/podcasts/{uid}/output")
	ResponseEntity<InputStreamResource> getOutputMedia(@PathVariable String uid) {
		return this.podcastRepository //
				.findByUid(uid)//
				.map(Podcast::getS3OutputFileName) //
				.map(s3Key -> new Object() {
					S3Object object = s3.downloadOutputFile(s3Key);

					String key = s3Key;

				})//
				.map(record -> this.doDownloadFile(record.object, uid, record.key)) //
				.orElseThrow(() -> new IllegalArgumentException(
						"couldn't find the Podcast associated with UID  " + uid));
	}

	@SneakyThrows
	private ResponseEntity<InputStreamResource> doDownloadFile(S3Object object,
			String uid, String key) {
		var inputStream = object.getObjectContent();
		var inputStreamResource = new InputStreamResource(inputStream);
		return ResponseEntity.ok()//
				.header("X-Podcast-Uid", uid).contentType(this.mediaContentType)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + key + "\"")
				.body(inputStreamResource);
	}

	@PostMapping("/podcasts/{uid}")
	ResponseEntity<?> beginProduction(@PathVariable("uid") String uid,
			@RequestParam("file") MultipartFile file) throws Exception {
		var newFile = new File(this.file, uid);
		file.transferTo(newFile);
		FileUtils.assertFileExists(newFile);
		Assert.isTrue(this.service.launchPipeline(uid, newFile), "the pipeline says no.");
		var location = URI.create("/podcasts/" + uid + "/status");
		log.info("sending status location as : '" + location + "'");
		return ResponseEntity.accepted().location(location).build();
	}

}
