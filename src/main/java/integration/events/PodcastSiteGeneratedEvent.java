package integration.events;

import org.springframework.context.ApplicationEvent;

import java.util.Date;

public class PodcastSiteGeneratedEvent extends ApplicationEvent {

	public PodcastSiteGeneratedEvent(Date source) {
		super(source);
	}

	@Override
	public Date getSource() {
		return (Date) super.getSource();
	}

}
