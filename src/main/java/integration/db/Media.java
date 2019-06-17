package integration.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.List;

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
