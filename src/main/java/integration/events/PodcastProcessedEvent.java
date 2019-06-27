package integration.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class PodcastProcessedEvent extends ApplicationEvent {

	private final String uid, bucketName, fileName;

	public PodcastProcessedEvent(String uid, String bucketName, String ext) {
		super(Map.of("uid", uid, "inputBucketName", bucketName, "extension", ext));
		this.bucketName = bucketName;
		this.uid = uid;
		this.fileName = uid + '.' + ext;
	}

}
