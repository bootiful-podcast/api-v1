package integration.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

@Log4j2
@RequiredArgsConstructor
public class AwsS3Service {

	private final String uploadBucketName;

	private final String outputBucketName;

	private final AmazonS3 s3;

	public URI createS3Uri(String bucketName, String nestedBucketFolder,
			String fileName) {
		var uri = this.s3FqnFor(bucketName, nestedBucketFolder, fileName);
		log.debug("the S3 FQN URI is " + uri);
		if (null == uri) {
			log.debug("the URI is null; returning null");
			return null;
		}
		return URI.create(uri);
	}

	public S3Object downloadOutputFile(String fn) {
		return this.download(this.outputBucketName, null, fn);
	}

	public S3Object download(String bucketName, String nestedBucketFolder, String key) {
		var bn = (nestedBucketFolder == null ? "" : "/" + nestedBucketFolder);
		var request = new GetObjectRequest(bucketName, bn + key);
		return this.s3.getObject(request);
	}

	@SneakyThrows
	public URI upload(String contentType, String nestedBucketFolder, File file) {
		if (file.length() > 0) {
			var objectMetadata = new ObjectMetadata();
			objectMetadata.setContentType(contentType);
			objectMetadata.setContentLength(file.length());
			try (var inputStream = new BufferedInputStream(new FileInputStream(file))) {
				var request = new PutObjectRequest(this.uploadBucketName
						+ (nestedBucketFolder == null ? "" : "/" + nestedBucketFolder),
						file.getName(), inputStream, objectMetadata);
				PutObjectResult putObjectResult = this.s3.putObject(request);
				Assert.notNull(putObjectResult, "the S3 file hasn't been uploaded");
				return this.createS3Uri(this.uploadBucketName, nestedBucketFolder,
						file.getName());
			}
		}
		return null;
	}

	private String s3FqnFor(String bucket, String folder, String fn) {
		Assert.notNull(bucket, "the bucket name can't be null");
		Assert.notNull(fn, "the file name can't be null");
		if (StringUtils.hasText(folder)) {
			if (!folder.endsWith("/")) {
				folder = folder + "/";
			}
		}
		String key = folder + fn;
		try {
			S3Object object = this.s3.getObject(new GetObjectRequest(bucket, key));
			Assert.notNull(object, "the fetch of the object should not be null");
		}
		catch (Exception e) {
			log.warn("No object of this key name " + key + "exists in this bucket, "
					+ bucket);
			return null;
		}
		return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
	}

}
