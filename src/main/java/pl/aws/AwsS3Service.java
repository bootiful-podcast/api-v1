package pl.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

@Log4j2
@RequiredArgsConstructor
public class AwsS3Service {

	private final String bucketName;

	private final AmazonS3 s3;

	public URI createS3Uri(String bucketName, String nestedBucketFolder,
			String fileName) {
		return URI
				.create("s3://" + bucketName + "/" + nestedBucketFolder + "/" + fileName);
	}

	@SneakyThrows
	public String upload(String contentType, String nestedBucketFolder, File file) {
		if (file.length() > 0) {
			var objectMetadata = new ObjectMetadata();
			objectMetadata.setContentType(contentType);
			objectMetadata.setContentLength(file.length());
			try (var inputStream = new BufferedInputStream(new FileInputStream(file))) {
				var request = new PutObjectRequest(this.bucketName
						+ (nestedBucketFolder == null ? "" : "/" + nestedBucketFolder),
						file.getName(), inputStream, objectMetadata);
				PutObjectResult putObjectResult = this.s3.putObject(request);
				Assert.notNull(putObjectResult, "the S3 file hasn't been uploaded");
				return this
						.createS3Uri(this.bucketName, nestedBucketFolder, file.getName())
						.toString();
			}
		}
		return null;
	}

}
