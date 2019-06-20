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
