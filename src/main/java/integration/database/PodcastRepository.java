package integration.database;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PodcastRepository extends CrudRepository<Podcast, Long> {

	// TODO this needs to find the LATEST byUid - not just all of the ones by UID.
	Optional<Podcast> findByUid(String uid);

}
