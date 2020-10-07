package integration.database;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "podcast_users", indexes = @Index(columnList = "username", unique = true))
public class User {

	@Id
	@GeneratedValue
	private Long id;

	private String username;

	private String password;

}
