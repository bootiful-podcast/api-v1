package integration.upload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequiredArgsConstructor
class AmazonS3Uploader {

	private String bucketName = "podcast-input-bucket";
	private final AmazonS3 amazonS3;

	@PostMapping("/s3")
	String uploadToAmazonS3(@RequestParam MultipartFile file) 	throws Throwable {

		if (!file.isEmpty()) {
			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setContentType(file.getContentType());

			PutObjectRequest request = new PutObjectRequest(this.bucketName, file.getName(),
				file.getInputStream(), objectMetadata)
				.withCannedAcl(CannedAccessControlList.PublicRead) ;

			this.amazonS3.putObject(request);
		}
		return "/";
	}
}
