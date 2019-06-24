package integration;

import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.utils.FileUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Log4j2
@RestController
class PipelineHttpController {

	private final File file;

	private final PipelineService service;

	private final PodcastRepository podcastRepository;

	private final String processingMessage = "processing";

	private final String finishedMessage = "finished processing";

	PipelineHttpController(PipelineProperties props, PodcastRepository repository,
			PipelineService service) {

		this.file = FileUtils.ensureDirectoryExists(props.getS3().getStagingDirectory());
		this.service = service;
		this.podcastRepository = repository;
	}

	@GetMapping("/podcasts/{uid}/status")
	ResponseEntity<?> getStatusForPodcast(@PathVariable String uid) {
		Optional<Podcast> byUid = podcastRepository.findByUid(uid);
		Optional<Map<?, ?>> response = byUid.map(podcast -> {
			if (null != podcast.getProductionArtifact()) {
				return Map.of("media-url", podcast.getProductionArtifact(), "status",
						this.finishedMessage);
			}
			else {
				return Map.of("status", this.processingMessage);
			}
		});
		return response.map(reply -> ResponseEntity.ok().body(reply))
				.orElse(ResponseEntity.noContent().build());
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
