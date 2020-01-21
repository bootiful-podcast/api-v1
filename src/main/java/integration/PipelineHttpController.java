package integration;

import com.amazonaws.services.s3.model.S3Object;
import integration.aws.AwsS3Service;
import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.utils.CopyUtils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;

@Log4j2
@RestController
class PipelineHttpController {

	private final File file;

	private final PipelineService service;

	private final AwsS3Service s3;

	private final PodcastRepository podcastRepository;

	private final String processingMessage = "processing";

	private final String finishedMessage = "finished processing";

	private final MediaType photoContentType = MediaType.IMAGE_JPEG;

	private final MediaType audioContentType = MediaType.parseMediaType("binary/octet-stream");

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

	private ResponseEntity<InputStreamResource> readResource(MediaType mediaContentType, String uid,
			Function<Podcast, String> functionToExtractAFileNameKeyGivenAPodcast,
			Function<String, S3Object> produceS3Object) {
		return this.podcastRepository //
				.findByUid(uid)//
				.map(functionToExtractAFileNameKeyGivenAPodcast) //
				.map(s3Key -> {
					try {
						return new Object() {
							S3Object object = produceS3Object.apply(s3Key);

							String key = s3Key;

						};
					}
					catch (Exception e) {
						ReflectionUtils.rethrowRuntimeException(e);
					}
					return null;
				})//
				.map(record -> {
					var inputStream = record.object.getObjectContent();
					var inputStreamResource = new InputStreamResource(inputStream);
					return ResponseEntity.ok()//
							.header("X-Podcast-UID", uid)//
							.contentType(mediaContentType)//
							.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.key + "\"")//
							.body(inputStreamResource);
				}) //
				.orElseThrow(
						() -> new IllegalArgumentException("couldn't find the Podcast associated with UID  " + uid));

	}

	@SneakyThrows
	@GetMapping("/podcasts/{uid}/profile-photo")
	ResponseEntity<InputStreamResource> getProfilePhotoMedia(@PathVariable String uid) {
		return this.readResource(this.photoContentType, uid, Podcast::getS3PhotoFileName,
				fileName -> s3.downloadInputFile(uid, fileName));
	}

	@SneakyThrows
	@GetMapping("/podcasts/{uid}/produced-audio")
	ResponseEntity<InputStreamResource> getProducedAudioMedia(@PathVariable String uid) {
		return this.readResource(this.audioContentType, uid, Podcast::getS3AudioFileName,
				fileName -> s3.downloadOutputFile(uid, fileName));
	}

	@PostMapping("/podcasts/{uid}")
	ResponseEntity<?> beginProduction(@PathVariable("uid") String uid, @RequestParam("file") MultipartFile file)
			throws Exception {
		var newFile = new File(this.file, uid);
		file.transferTo(newFile);
		CopyUtils.assertFileExists(newFile);
		log.info("the newly POST'd file lives at " + newFile.getAbsolutePath() + '.');
		Assert.isTrue(this.service.launchPipeline(uid, newFile), "the pipeline says no.");
		var location = URI.create("/podcasts/" + uid + "/status");
		log.info("sending status location as : '" + location + "'");
		return ResponseEntity.accepted().location(location).build();
	}

}
