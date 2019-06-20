package integration;

import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Data
@AllArgsConstructor
@Log4j2
@NoArgsConstructor
public class PodcastPackageManifest {

	private String title, description, uid;

	public Collection<Media> getMedia() {
		return new ArrayList<>(this.media);
	}

	private Collection<Media> media = new ArrayList<>();

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class Media {

		private String interview, introduction, extension;

	}

	private static String readAttributeFrom(String attr, NamedNodeMap map) {
		if (map == null || map.getLength() == 0) {
			return null;
		}
		var namedItem = map.getNamedItem(attr);
		if (namedItem != null) {
			var textContent = namedItem.getTextContent();
			if (StringUtils.hasText(textContent)) {
				return textContent;
			}
		}
		return null;
	}

	private static Media readMedia(NodeList nodeList, String ext) {
		var ok = (nodeList != null && nodeList.getLength() > 0);
		if (!ok) {
			return null;
		}
		var first = nodeList.item(0);
		var attributes = first.getAttributes();
		var interview = readAttributeFrom("interview", attributes);
		var intro = readAttributeFrom("intro", attributes);
		return new Media(interview, intro, ext);
	}

	@SneakyThrows
	public static PodcastPackageManifest from(File manifest) {
		var dbf = DocumentBuilderFactory.newInstance();
		var db = dbf.newDocumentBuilder();
		var doc = db.parse(manifest);
		var build = new PodcastPackageManifest();
		var podcast = doc.getElementsByTagName("podcast");
		Assert.isTrue(podcast.getLength() > 0,
				"there must be at least one podcast element in a manifest");
		var attributes = podcast.item(0).getAttributes();
		build.setDescription(readAttributeFrom("description", attributes));
		build.setUid(readAttributeFrom("uid", attributes));
		build.setTitle(readAttributeFrom("title", attributes));
		List.of("mp3,wav".split(",")).forEach(
				ext -> getMediaFromDoc(doc, ext).ifPresent(x -> build.media.add(x)));
		return build;
	}

	private static Optional<Media> getMediaFromDoc(Document doc, String ext) {
		var media = readMedia(doc.getElementsByTagName(ext), ext);
		return Optional.ofNullable(media);
	}

}
