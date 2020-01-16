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

	@Column(name = "s3_fqn_uri")
	private String mediaS3Uri;

	@Column(name = "s3_output_file_name")
	private String s3OutputFileName;

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
	//@formatter:off
    return "Podcast{" +
        "id=" + id +
        ", uid='" + uid + '\'' +
        ", title='" + title + '\'' +
        ", description='" + description + '\'' +
        ", notes='" + notes + '\'' +
        ", transcript='" + transcript + '\'' +
        ", mediaS3Uri='" + mediaS3Uri + '\'' +
        ", s3OutputFileName='" + s3OutputFileName + '\'' +
        ", podbeanDraftCreated=" + podbeanDraftCreated +
        ", podbeanDraftPublished=" + podbeanDraftPublished +
        ", date=" + date +
        '}';
    //@formatter:on
	}

}
