package integration.database;

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
public class Podcast {

	@Id
	@GeneratedValue
	private Long id;

	private String uid; // this is to support correlation later on

	private String title, description, notes, transcript;

	@Column(name = "s3_audio_uri")
	private String s3AudioUri;

	@Column(name = "s3_photo_uri")
	private String s3PhotoUri;

	@Column(name = "s3_audio_file_name")
	private String s3AudioFileName;

	@Column(name = "s3_photo_file_name")
	private String s3PhotoFileName;

	@Column(name = "podbean_draft_created")
	private Date podbeanDraftCreated;

	@Column(name = "podbean_draft_published")
	private Date podbeanDraftPublished;

	@Column(name = "podbean_media_uri")
	private String podbeanMediaUri;

	private Date date = new Date();

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "podcast_link", joinColumns = @JoinColumn(name = "podcast_id"),
			inverseJoinColumns = @JoinColumn(name = "link_id"))
	private List<Link> links = new ArrayList<>();

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "podcast_media", joinColumns = @JoinColumn(name = "podcast_id"),
			inverseJoinColumns = @JoinColumn(name = "media_id"))
	private List<Media> media = new ArrayList<>();

	@Override
	public String toString() {
		return "Podcast{" + "id=" + id + ", uid='" + uid + '\'' + ", title='" + title + '\'' + ", description='"
				+ description + '\'' + ", notes='" + notes + '\'' + ", transcript='" + transcript + '\''
				+ ", mediaS3Uri='" + s3AudioUri + '\'' + ", s3OutputFileName='" + s3AudioFileName + '\''
				+ ", s3EpisodePhoto='" + s3PhotoFileName + '\'' + ", podbeanDraftCreated=" + podbeanDraftCreated
				+ ", podbeanDraftPublished=" + podbeanDraftPublished + ", podbeanMediaUri='" + podbeanMediaUri + '\''
				+ ", date=" + date + ", links=" + links + ", media=" + media + '}';
	}

}
