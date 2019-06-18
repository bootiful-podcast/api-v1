package pl;

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

@Data
@AllArgsConstructor
@Log4j2
@NoArgsConstructor
class UploadPackageManifest {

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class Media {

		private String interview, introduction, extension;

	}

	private String description, uid;

	public Collection<Media> getMedia() {
		return new ArrayList<>(this.media);
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

	private Collection<Media> media = new ArrayList<>();

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
	static UploadPackageManifest from(File manifest) {

		var dbf = DocumentBuilderFactory.newInstance();
		var db = dbf.newDocumentBuilder();
		var doc = db.parse(manifest);
		var build = new UploadPackageManifest();
		var podcast = doc.getElementsByTagName("podcast");
		Assert.isTrue(podcast.getLength() > 0,
				"there must be at least one podcast element in a manifest");
		var attributes = podcast.item(0).getAttributes();
		build.setDescription(readAttributeFrom("description", attributes));
		build.setUid(readAttributeFrom("uid", attributes));
		String[] exts = "mp3,wav".split(",");
		for (String ext : exts) {
			Media mediaFromDoc = getMediaFromDoc(doc, ext);
			if (null != mediaFromDoc) {
				build.media.add(mediaFromDoc);
			}
		}
		return build;
	}

	private static Media getMediaFromDoc(Document doc, String mp3Ext) {
		return readMedia(doc.getElementsByTagName(mp3Ext), mp3Ext);
	}

}
