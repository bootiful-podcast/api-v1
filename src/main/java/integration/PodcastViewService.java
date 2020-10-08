package integration;

import com.joshlong.templates.MarkdownService;
import integration.database.Podcast;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class PodcastViewService {

	private final MarkdownService markdownService;

	Collection<PodcastView> from(Iterable<Podcast> podcasts) {
		var out = new ArrayList<Podcast>();
		podcasts.forEach(out::add);
		return out.parallelStream()
				.map(p -> PodcastView.from(p, this.markdownService.convertMarkdownTemplateToHtml(p.getDescription())))
				.collect(Collectors.toList());
	}

}
