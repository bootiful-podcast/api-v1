package integration.db;

import org.springframework.data.repository.CrudRepository;

interface PodcastRepository extends CrudRepository<Podcast, Long> {

}
