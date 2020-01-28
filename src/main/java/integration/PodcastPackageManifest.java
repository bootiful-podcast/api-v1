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
import org.w3c.dom.Node;

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

	public static PodcastPackageManifest from(String uid, String title, String description, String introFileName,
			String interviewFileName, String photoFileName) {
		var pm = new PodcastPackageManifest();
		pm.description = description;
		pm.uid = uid;
		pm.title = title;
		pm.getInterview().src = interviewFileName;
		pm.getIntroduction().src = introFileName;
		pm.getPhoto().src = photoFileName;
		return pm;
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
		var introSrc = XmlUtils.getAttribute(intro, "src");
		var interviewSrc = XmlUtils.getAttribute(interview, "src");
		var photoSrc = XmlUtils.getAttribute(photo, "src");
		var descriptionTxt = description.getTextContent().trim();
		var uid = XmlUtils.getAttribute(podcastElement, "uid");
		var title = XmlUtils.getAttribute(podcastElement, "title");
		return from(uid, title, descriptionTxt, introSrc, interviewSrc, photoSrc);
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