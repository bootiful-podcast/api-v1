package pl.events;

import org.springframework.context.ApplicationEvent;
import pl.PodcastPackageManifest;

public class PodcastArchiveUploadedEvent extends ApplicationEvent {

	@Override
	public PodcastPackageManifest getSource() {
		return (PodcastPackageManifest) super.getSource();
	}

	public PodcastArchiveUploadedEvent(PodcastPackageManifest source) {
		super(source);
	}

}
