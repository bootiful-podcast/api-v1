package integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

@Data
@AllArgsConstructor
@Log4j2
@NoArgsConstructor
public class PodcastPackageManifest {

	private String title, description, uid;

	private Interview interview = new Interview();

	private Introduction introduction = new Introduction();

	private Photo photo = new Photo();

	@SneakyThrows
	public static PodcastPackageManifest from(File file) {
		try (var fin = new BufferedInputStream(new FileInputStream(file))) {
			return from(fin);
		}
	}

	@SneakyThrows
	public static PodcastPackageManifest from(InputStream in) {
		var dbf = DocumentBuilderFactory.newInstance();
		var documentBuilder = dbf.newDocumentBuilder();
		var doc = documentBuilder.parse(in);

		var podcastElement = (Element) doc.getElementsByTagName("podcast").item(0);

		var interview = podcastElement.getElementsByTagName("interview").item(0);
		var intro = podcastElement.getElementsByTagName("introduction").item(0);
		var photo = podcastElement.getElementsByTagName("photo").item(0);
		var description = podcastElement.getElementsByTagName("description").item(0);

		Arrays.asList(interview, intro, photo, description)
				.forEach(e -> Assert.notNull(e, "the element must not be null"));

		var build = new PodcastPackageManifest();
		build.getIntroduction().src = readAttributeFrom(
				Objects.requireNonNull(intro).getAttributes(), "src");
		build.getInterview().src = readAttributeFrom(
				Objects.requireNonNull(interview).getAttributes(), "src");
		build.getPhoto().src = readAttributeFrom(
				Objects.requireNonNull(photo).getAttributes(), "src");

		build.description = description.getTextContent().trim();
		build.uid = readAttributeFrom(podcastElement.getAttributes(), "uid");
		build.title = readAttributeFrom(podcastElement.getAttributes(), "title");

		return build;

	}

	private static Element elementWithin(Element element, String tagName) {
		var childNodes = element.getChildNodes();
		var length = childNodes.getLength();
		for (var i = 0; i < length; i++) {
			var node = childNodes.item(i);
			if (node instanceof Element) {
				var nodeElement = (Element) node;
				if (nodeElement.getTagName().equalsIgnoreCase(tagName))
					return nodeElement;
			}
		}
		return null;
	}

	private static String readAttributeFrom(NamedNodeMap map, String attr) {
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

	@Data
	public static class Interview {

		private String src = "";

	}

	@Data
	public static class Introduction {

		private String src = "";

	}

	@Data
	public static class Photo {

		private String src = "";

	}

}