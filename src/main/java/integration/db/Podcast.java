package integration.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "podcast_link", joinColumns = @JoinColumn(name = "podcast_id"),
			inverseJoinColumns = @JoinColumn(name = "link_id"))
	private List<Link> links = new ArrayList<>();

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "podcast_media", joinColumns = @JoinColumn(name = "podcast_id"),
			inverseJoinColumns = @JoinColumn(name = "media_id"))
	private List<Media> media = new ArrayList<>();

}
