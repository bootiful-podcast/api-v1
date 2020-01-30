package integration;

import integration.utils.XmlUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnproducedPodcastPackageManifest extends PodcastPackageManifest {

	private Interview interview = new Interview();

	private Introduction introduction = new Introduction();

	@SneakyThrows
	public static UnproducedPodcastPackageManifest from(File file) {
		try (var fin = new BufferedInputStream(new FileInputStream(file))) {
			return from(fin);
		}
	}

	@SneakyThrows
	public static UnproducedPodcastPackageManifest from(InputStream in) {
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

	public static UnproducedPodcastPackageManifest from(String uid, String title, String description,
			String introFileName, String interviewFileName, String photoFileName) {
		var pm = new UnproducedPodcastPackageManifest();
		pm.description = description;
		pm.uid = uid;
		pm.title = title;
		pm.getInterview().setSrc(interviewFileName);
		pm.getIntroduction().setSrc(introFileName);
		pm.getPhoto().setSrc(photoFileName);
		return pm;
	}

}
