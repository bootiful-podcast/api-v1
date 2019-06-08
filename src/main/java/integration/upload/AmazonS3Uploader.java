package integration.upload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
class AmazonS3Uploader {

	// this is the bucket where assets to be processed will live
	private final AmazonS3 amazonS3;

	private final String inputBucketName;

	AmazonS3Uploader(@Value("${podcast.input-bucket-name}") String bucketName,
			AmazonS3 amazonS3) {
		this.inputBucketName = bucketName;
		this.amazonS3 = amazonS3;
	}

	@PostMapping("/s3")
	String uploadToAmazonS3(@RequestParam MultipartFile file) throws Throwable {

		if (!file.isEmpty()) {
			var objectMetadata = new ObjectMetadata();
			objectMetadata.setContentType(file.getContentType());

			var request = new PutObjectRequest(this.inputBucketName,
					file.getOriginalFilename(), file.getInputStream(), objectMetadata);

			this.amazonS3.putObject(request);
		}
		return "/";
	}

}
