package integration.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@SpringBootApplication
public class PodcastDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(PodcastDbApplication.class, args);
	}
}

interface PodcastRepository extends CrudRepository<Podcast, Long> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
class Media {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToMany(mappedBy = "media")
	private List<Podcast> podcasts = new ArrayList<>();
	private String href, description, extension, type;
}

@Component
@RequiredArgsConstructor
class Initializer {

	private final PodcastRepository repository;

	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		var podcast = new Podcast(null, "title", "description", "notes", "transcript",
				new Date(), Collections.emptyList(), Collections.emptyList());
		repository.save(podcast);
	}

}

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
class Podcast {

	@Id
	@GeneratedValue
	private Long id;

	private String title, description, notes, transcript;

	private Date date;

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "podcast_link", joinColumns = @JoinColumn(name = "podcast_id"),
			inverseJoinColumns = @JoinColumn(name = "link_id"))
	private List<Link> links = new ArrayList<>();

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "podcast_media", joinColumns = @JoinColumn(name = "podcast_id"),
			inverseJoinColumns = @JoinColumn(name = "media_id"))
	private List<Media> media = new ArrayList<>();

}

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
class Link {

	@Id
	@GeneratedValue
	private Long id;

	private String href, description;

	@ManyToMany(mappedBy = "links")
	private List<Podcast> podcasts = new ArrayList<>();

}