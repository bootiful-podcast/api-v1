package integration.soundcloud.migration;

import com.amazonaws.services.identitymanagement.model.AddClientIDToOpenIDConnectProviderRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@Log4j2
@Component
class SoundcloudDownloaderJob {

	private final URI uri = URI.create("http://feeds.soundcloud.com/users/soundcloud:users:521839740/sounds.rss");

	private final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

	@SneakyThrows
	@EventListener(ApplicationReadyEvent.class)
	public void downloadAllAssetsFromSoundcloud() throws Exception {
		var urlResource = new UrlResource(uri);
		var dbf = DocumentBuilderFactory.newInstance();
		var documentBuilder = dbf.newDocumentBuilder();
		var finalPodcasts = new ArrayList<SoundcloudPodcast>();
		try (var inputStream = urlResource.getInputStream()) {
			var doc = documentBuilder.parse(inputStream);
			NodeList items = doc.getElementsByTagName("item");
			for (var i = 0; i < items.getLength(); i++) {
				var currentItem = items.item(i);
				finalPodcasts.add(buildFromItem((Element) currentItem));
			}
		}
		finalPodcasts.forEach(log::info);
	}

	@SneakyThrows
	private SoundcloudPodcast buildFromItem(Element node) {
		var guid = node.getElementsByTagName("guid").item(0).getTextContent();
		var title = node.getElementsByTagName("title").item(0).getTextContent();
		var pubDate = dateFormat.parse(node.getElementsByTagName("pubDate").item(0).getTextContent());
		var linkToSoundcloudEpisode = URI.create(node.getElementsByTagName("link").item(0).getTextContent());
		var description = node.getElementsByTagName("description").item(0).getTextContent();
		var mp3EnclosureNode = node.getElementsByTagName("enclosure").item(0);
		var mp3EnclosureType = readAttributeFrom(mp3EnclosureNode.getAttributes(), "type");
		Assert.isTrue(mp3EnclosureType.equalsIgnoreCase("audio/mpeg"), "the media file needs to be an .mp3");
		var mp3EnclosureUri = URI.create(readAttributeFrom(mp3EnclosureNode.getAttributes(), "url"));
		Assert.notNull(mp3EnclosureUri, "you must provide a valid .mp3 URI from SoundCloud");
		var itunesImageNode = node.getElementsByTagName("itunes:image").item(0);
		var itunesImageNodeUri = URI.create(readAttributeFrom(itunesImageNode.getAttributes(), "href"));
		return new SoundcloudPodcast(guid, title, pubDate, linkToSoundcloudEpisode, description, mp3EnclosureUri,
				itunesImageNodeUri);
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

}

@Data
@RequiredArgsConstructor
class SoundcloudPodcast {

	private final String guid, title;

	private final Date pubDate;

	private final URI linkToSoundcloudEpisode;

	private final String description;

	private final URI mp3Uri, imageUri;

}