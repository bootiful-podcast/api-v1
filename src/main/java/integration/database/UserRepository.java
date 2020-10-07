package integration.database;

import org.springframework.data.repository.CrudRepository;

import java.util.Collection;

public interface UserRepository extends CrudRepository<User, Long> {

	Collection<User> findByUsername(String username);

}
