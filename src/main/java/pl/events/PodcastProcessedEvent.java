package pl.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class PodcastProcessedEvent extends ApplicationEvent {

	private final String uid, bucketName, fileName;

	public PodcastProcessedEvent(String uid, String bucketName, String fileName) {
		super(Map.of("uid", uid, "bucketName", bucketName, "fileName", fileName));
		this.bucketName = bucketName;
		this.uid = uid;
		this.fileName = fileName;
	}

}
