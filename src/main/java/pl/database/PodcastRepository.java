package pl.database;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

interface PodcastRepository extends CrudRepository<Podcast, Long> {

	Optional<Podcast> findByUid(String uid);

}
