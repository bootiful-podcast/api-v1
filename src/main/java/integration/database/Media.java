package integration.database;

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

	@Id
	@GeneratedValue
	private Long id;

	@ManyToMany(mappedBy = "media")
	private List<Podcast> podcasts = new ArrayList<>();

	private String href, fileName, description, extension, type;

}
