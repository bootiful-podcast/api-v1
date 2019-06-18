package pl;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import java.io.File;
import java.util.UUID;

// todo remove this
@Deprecated
@Log4j2
@Configuration
class Demo {

	private final PipelineService pipelineService;

	Demo(PipelineService service) {
		this.pipelineService = service;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void go() {
		var uuid = UUID.randomUUID().toString();
		var file = "/Users/joshlong/Desktop/pkg.zip".trim();
		var sent = this.pipelineService.launchPipelineForPodcastPackage(uuid,
				new File(file));
		Assert.isTrue(sent, "the pipeline should have started by now.");
	}

}
