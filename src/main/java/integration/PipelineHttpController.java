package integration;

import integration.aws.AwsS3Service;
import integration.database.PodcastRepository;
import integration.utils.CopyUtils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
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

	private final String accessControlAllowOriginHeaderValue = "https://bootifulpodcast.fm";

	private final AwsS3Service s3;

	private final PodcastRepository podcastRepository;

	private final String processingMessage = "processing";

	private final String finishedMessage = "finished processing";

	private final MediaType photoContentType = MediaType.IMAGE_JPEG;

	private final MediaType audioContentType = MediaType.parseMediaType("audio/mpeg");

	// parseMediaType("binary/octet-stream");

	PipelineHttpController(PipelineProperties props, AwsS3Service s3, PodcastRepository repository,
			PipelineService service) {
		this.file = CopyUtils.ensureDirectoryExists(props.getS3().getStagingDirectory());
		this.service = service;
		this.s3 = s3;
		this.podcastRepository = repository;
	}

	@GetMapping("/podcasts/{uid}/status")
	ResponseEntity<?> getStatusForPodcast(@PathVariable String uid) {
		var byUid = podcastRepository.findByUid(uid);
		var response = byUid.map(podcast -> {
			Map<String, String> statusMap;
			if (null != podcast.getS3AudioUri()) {
				statusMap = Map.of( //
						"media-url", service.buildMediaUriForPodcastById(podcast.getId()).toString(), //
						"status", this.finishedMessage //
				);
			}
			else {
				statusMap = Map.of("status", this.processingMessage);
			}
			log.info("returning status: " + statusMap.toString() + " for " + uid);
			return statusMap;
		});
		return response.map(reply -> ResponseEntity.ok().body(reply)).orElse(ResponseEntity.noContent().build());
	}

	@SneakyThrows
	@GetMapping("/podcasts/{uid}/profile-photo")
	ResponseEntity<Resource> getProfilePhotoMedia(@PathVariable String uid) {
		PipelineService.S3Resource podcastPhotoMedia = service.getPodcastPhotoMedia(uid);
		return ResponseEntity.ok()//
				.header("X-Podcast-UID", uid)//
				.header(HttpHeaders.ACCEPT_RANGES, "none").contentType(this.photoContentType)//
				.contentLength(podcastPhotoMedia.contentLength())
				// .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
				// uid + ".jpg" + "\"")//
				.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, this.accessControlAllowOriginHeaderValue)
				.body(podcastPhotoMedia);
	}

	@SneakyThrows
	@GetMapping({ "/podcasts/{uid}/produced-audio", "/podcasts/{uid}/produced-audio.mp3" })
	ResponseEntity<Resource> getProducedAudioMedia(@PathVariable String uid) {
		PipelineService.S3Resource podcastAudioMedia = service.getPodcastAudioMedia(uid);
		return ResponseEntity.ok()//
				.header("X-Podcast-UID", uid)//
				.contentType(this.audioContentType)//
				.contentLength(podcastAudioMedia.contentLength())//
				.header(HttpHeaders.ACCEPT_RANGES, "none")//
				.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, this.accessControlAllowOriginHeaderValue)
				.body(podcastAudioMedia);
	}

	@PostMapping("/podcasts/{uid}")
	ResponseEntity<?> beginProduction(@PathVariable("uid") String uid, @RequestParam("file") MultipartFile file)
			throws Exception {
		var newFile = new File(this.file, uid);
		file.transferTo(newFile);
		CopyUtils.assertFileExists(newFile);
		log.info("the newly POST'd file lives at " + newFile.getAbsolutePath() + '.');
		Assert.isTrue(this.service.launchProcessorPipeline(uid, newFile), "the pipeline says no.");
		var location = URI.create("/podcasts/" + uid + "/status");
		log.info("sending status location as : '" + location + "'");
		return ResponseEntity.accepted().location(location).build();
	}

}
