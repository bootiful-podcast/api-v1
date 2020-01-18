package integration.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.URI;

@Log4j2
@RequiredArgsConstructor
public class AwsS3Service {

	private final String inputBucketName;

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

	public S3Object downloadInputFile(String folder, String fn) {
		return this.download(this.inputBucketName, folder, fn);
	}

	public S3Object downloadOutputFile(String folder, String fn) {
		return this.download(this.outputBucketName, folder, fn);
	}

	public S3Object download(String bucketName, String folder, String key) {
		try {
			log.info(String.format(
					"trying to download from bucket %s with key based on folder %s and key %s",
					bucketName, folder, key));
			var newKey = folder == null ? key : folder + '/' + key;
			var request = new GetObjectRequest(bucketName, newKey);
			return this.s3.getObject(request);
		}
		catch (AmazonS3Exception e) {
			return download(bucketName, null, key);
		}
	}

	@SneakyThrows
	public URI uploadInputFile(String contentType, String nestedBucketFolder, File file) {
		return this.upload(this.inputBucketName, contentType, nestedBucketFolder, file);
	}

	@SneakyThrows
	public URI uploadOutputFile(String contentType, String folder, File file) {
		return this.upload(this.outputBucketName, contentType, folder, file);
	}

	@SneakyThrows
	private URI upload(String bucketName, String contentType, String nestedBucketFolder,
			File file) {
		if (file.length() > 0) {
			log.info("trying to upload the file " + file.getAbsolutePath() + " to bucket "
					+ bucketName + " with content type " + contentType
					+ " and nested folder " + nestedBucketFolder);
			var objectMetadata = new ObjectMetadata();
			objectMetadata.setContentType(contentType);
			objectMetadata.setContentLength(file.length());
			var request = new PutObjectRequest(bucketName
					+ (nestedBucketFolder == null ? "" : "/" + nestedBucketFolder),
					file.getName(), file);
			var putObjectResult = this.s3.putObject(request);
			Assert.notNull(putObjectResult, "the S3 file hasn't been uploaded");
			return this.createS3Uri(bucketName, nestedBucketFolder, file.getName());
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
			var object = this.s3.getObject(new GetObjectRequest(bucket, key));
			Assert.notNull(object, "the fetch of the object should not be null");
		}
		catch (Exception e) {
			log.warn("No object of this key name " + key + "exists in this bucket, "
					+ bucket);
			return null;
		}
		return String.format("s3://%s/%s", bucket, key);
	}

}
