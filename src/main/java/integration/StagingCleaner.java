package integration;

import integration.events.PodcastArtifactsUploadedToProcessorEvent;
import integration.utils.FileUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;

@Component
class StagingCleaner {

	@EventListener
	public void cleanUpStagingDirectory(PodcastArtifactsUploadedToProcessorEvent event) {

		File stagingDirectory = event.getSource().getStagingDirectory();

		Assert.isTrue(
				!stagingDirectory.exists()
						|| FileUtils.deleteDirectoryRecursively(stagingDirectory),
				"we couldn't delete the staging directory. "
						+ "this could imperil our free space.");
	}

}
