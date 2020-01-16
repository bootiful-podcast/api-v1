package integration.events;

import integration.PodcastPackageManifest;
import org.springframework.context.ApplicationEvent;
import integration.OldPodcastPackageManifest;

public class PodcastArchiveUploadedEvent extends ApplicationEvent {

	@Override
	public PodcastPackageManifest getSource() {
		return (PodcastPackageManifest) super.getSource();
	}

	public PodcastArchiveUploadedEvent(PodcastPackageManifest source) {
		super(source);
	}

}
