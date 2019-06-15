package integration.db;

import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootApplication
public class PodcastDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(PodcastDbApplication.class, args);
	}

	@Bean
	TransactionTemplate transactionTemplate(PlatformTransactionManager txm) {
		return new TransactionTemplate(txm);
	}
}

interface PodcastRepository extends CrudRepository<Podcast, Long> {
}

@Component
@RequiredArgsConstructor
class Initializer {

	private final PodcastRepository repository;

	private final TransactionTemplate template;

	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		this.template.execute(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				var links = List.of(
					Link.builder().description("Cornelia on Twitter").href("http://twitter.com/cfdavisaf").build(),
					Link.builder().description("Cloud Native Patterns").href("https://www.manning.com/books/cloud-native-patterns").build()
				);

				var media = List.of(
					Media.builder().type(Media.TYPE_INTERVIEW).href("http://s3.com/media/cornelia_davis/2322-interview.wav").extension(Media.EXTENSION_WAV).build(),
					Media.builder().type(Media.TYPE_INTRODUCTION).href("http://s3.com/media/cornelia_davis/2322-introduction.wav").extension(Media.EXTENSION_WAV).build()
				);

				var podcast = Podcast.builder()
					.media(media)
					.links(links)
					.title("Cornelia Davis on  her new book 'Cloud Native Patterns'")
					.description("Join Pivotal SVP Cornelia Davis for a look at what it is to build cool things")
					.transcript("TODO")
					.notes("I couldn't find that photo I took of us together so I just used her Twitter profile picture (with her permission)")
					.build();

				repository.save(podcast);
			}
		});

	}


}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
class Media {

	public static final String EXTENSION_WAV = "wav";
	public static final String EXTENSION_MP3 = "mp3";

	public static final String TYPE_INTRODUCTION = "introduction";
	public static final String TYPE_INTERVIEW = "interview";

	@Id
	@GeneratedValue
	private Long id;

	@ManyToMany(mappedBy = "media")
	private List<Podcast> podcasts = new ArrayList<>();

	private String href, description, extension, type;


}

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
class Podcast {

	@Id
	@GeneratedValue
	private Long id;

	private String title, description, notes, transcript;

	private Date date = new Date();

	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(name = "podcast_link", joinColumns = @JoinColumn(name = "podcast_id"),
		inverseJoinColumns = @JoinColumn(name = "link_id"))
	private List<Link> links = new ArrayList<>();

	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(name = "podcast_media", joinColumns = @JoinColumn(name = "podcast_id"),
		inverseJoinColumns = @JoinColumn(name = "media_id"))
	private List<Media> media = new ArrayList<>();

}

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
class Link {

	@Id
	@GeneratedValue
	private Long id;

	private String href, description;

	@ManyToMany(mappedBy = "links")
	private List<Podcast> podcasts = new ArrayList<>();

}