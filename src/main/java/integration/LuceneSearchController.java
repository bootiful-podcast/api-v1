package integration;

import com.joshlong.lucene.DocumentWriteMapper;
import com.joshlong.lucene.LuceneTemplate;
import integration.database.Podcast;
import integration.database.PodcastRepository;
import integration.events.PodcastPublishedToPodbeanEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.document.*;
import org.apache.lucene.index.Term;
import org.jsoup.Jsoup;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequiredArgsConstructor
class LuceneSearchController {

	private final LuceneTemplate luceneTemplate;

	private final RestTemplate restTemplate;

	private final int maxResults = 1000;

	private final Object monitor = new Object();

	private final Map<String, Podcast> podcasts = new ConcurrentHashMap<>();

	// private final Collection<Podcast> podcasts = new ConcurrentLinkedQueue<>();
	private final PodcastRepository repository;

	@GetMapping("/podcasts/search")
	Collection<Podcast> search(@RequestParam String query) throws Exception {
		var idsThatMatch = searchIndex(query, maxResults);
		var out = this.podcasts.values().stream().filter(p -> idsThatMatch.contains(p.getUid()))
				.collect(Collectors.toList());
		log.debug("idsThatMatch=[" + idsThatMatch + "] query=[" + query + "] && " + "results.size = " + out.size());
		return out;
	}

	@PostMapping("/podcasts/index")
	ResponseEntity<?> refreshIndex() {
		refresh();
		return ResponseEntity.ok().build();
	}

	@SneakyThrows
	private Document buildPodcastDocument(Podcast podcast) {
		var document = new Document();
		document.add(new StringField("id", Long.toString(podcast.getId()), Field.Store.YES));
		document.add(new StringField("uid", podcast.getUid(), Field.Store.YES));
		document.add(new TextField("title", podcast.getTitle(), Field.Store.YES));
		document.add(new TextField("description", html2text(podcast.getDescription()), Field.Store.YES));
		document.add(new LongPoint("time", podcast.getDate().getTime()));
		return document;
	}

	private String html2text(String html) {
		return Jsoup.parse(html).text();
	}

	private List<String> searchIndex(String queryStr, int maxResults) throws Exception {
		return luceneTemplate.search(queryStr, maxResults, document -> document.get("uid"));
	}

	@Deprecated
	private Iterable<Podcast> loadPodcasts() {
		var podcastsJsonUri = URI.create("http://bootifulpodcast.fm/podcasts.json");
		var responseEntity = this.restTemplate.exchange(podcastsJsonUri, HttpMethod.GET, null,
				new ParameterizedTypeReference<Collection<Podcast>>() {
				});
		Assert.isTrue(responseEntity.getStatusCode().is2xxSuccessful(), () -> "the HTTP response should be 200x");
		return responseEntity.getBody();
	}

	@EventListener(PodcastPublishedToPodbeanEvent.class)
	public void newPodcast() {
		this.refresh();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void ready() {
		this.refresh();
	}

	private void refresh() {
		synchronized (this.monitor) {
			if (log.isInfoEnabled()) {
				log.info("refresh(): there are " + podcasts.size() + " episodes");
			}
			var podcasts = this.repository.findAll();
			for (var p : podcasts) {
				var podcast = new Podcast(p.getId(), p.getUid(), p.getTitle(), p.getDescription(), "", "",
						p.getS3AudioUri(), p.getS3PhotoUri(), p.getS3AudioFileName(), p.getS3PhotoFileName(),
						p.getPodbeanDraftCreated(), p.getPodbeanDraftPublished(), p.getPodbeanMediaUri(),
						p.getPodbeanPhotoUri(), p.getDate(), Collections.emptyList(), Collections.emptyList());
				this.podcasts.put(p.getUid(), podcast);
				log.info(p);
			}
			this.luceneTemplate.write(podcasts, podcast -> {
				var doc = buildPodcastDocument(podcast);
				return new DocumentWriteMapper.DocumentWrite(new Term("uid", podcast.getUid()), doc);
			});
		}
	}

}
