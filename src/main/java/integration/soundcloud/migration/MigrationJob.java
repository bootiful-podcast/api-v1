package integration.soundcloud.migration;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class MigrationJob {

	@EventListener(ApplicationReadyEvent.class)
	public void go() throws Exception {
	}

}
