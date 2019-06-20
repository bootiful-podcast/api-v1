package pl.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import pl.MediaTypes;

import java.util.List;

//TODO
@Log4j2
@Deprecated
@RequiredArgsConstructor
class Demo {

	private final PodcastRepository repository;

	private final TransactionTemplate template;

	/*
	 * @EventListener(ApplicationReadyEvent.class) public void init() { Podcast saved =
	 * this.template.execute(status -> {
	 *
	 * var links = List.of( Link.builder().description("Cornelia on Twitter")
	 * .href("http://twitter.com/cfdavisaf").build(),
	 * Link.builder().description("Cloud Native Patterns")
	 * .href("https://www.manning.com/books/cloud-native-patterns") .build());
	 *
	 * var media = List.of( Media.builder().type(MediaTypes.TYPE_INTERVIEW)
	 * .href("http://s3.com/media/cornelia_davis/2322-interview.wav")
	 * .extension(Media.EXTENSION_WAV).build(),
	 * Media.builder().type(Media.TYPE_INTRODUCTION).href(
	 * "http://s3.com/media/cornelia_davis/2322-introduction.wav")
	 * .extension(Media.EXTENSION_WAV).build());
	 *
	 * var podcast = Podcast.builder().media(media).links(links)
	 * .title("Cornelia Davis on  her new book 'Cloud Native Patterns'") .description(
	 * "Join Pivotal SVP Cornelia Davis for a look at what it is to build cool things")
	 * .transcript("TODO")
	 * .notes("I couldn't find that photo I took of us together so I just used her Twitter profile picture (with her permission)"
	 * ) .build();
	 *
	 * return repository.save(podcast); });
	 *
	 * log.info("saved " + Podcast.class.getName() + "#" + saved.getId());
	 *
	 * }
	 */

}
