package integration.soundcloud.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import integration.XmlUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

@Log4j2
@Component
@Profile("soundcloud-download")
class SoundcloudDownloaderJob {

	private final URI uri = URI.create("http://feeds.soundcloud.com/users/soundcloud:users:521839740/sounds.rss");

	private final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

	private final RestTemplate restTemplate = new RestTemplateBuilder().build();

	private final File root = new File(new File(System.getProperty("user.home"), "Desktop"), "soundcloud");

	private final ObjectMapper objectMapper;

	SoundcloudDownloaderJob(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		ensureDirectoryExists(this.root);
		log.info("files will be downloaded to " + this.root.getAbsolutePath() + '.');
	}

	private File mp3FileFor(SoundcloudPodcast podcast) {
		return contextualFile(podcast, "audio.mp3");
	}

	private String buildUniqueFileNameForPodcast(SoundcloudPodcast podcast) {
		var guid = podcast.getTitle();
		var len = guid.length();
		var str = new StringBuilder();
		for (var i = 0; i < len; i++) {
			var c = guid.charAt(i);
			if (Character.isLetterOrDigit(c) && !Character.isSpaceChar(c)) {
				str.append(c);
			}
		}
		return str.toString();
	}

	private File imageFileFor(SoundcloudPodcast podcast) {
		var imageUri = podcast.getImageUri().toString().toLowerCase();
		if (imageUri.contains(".")) {
			var parts = imageUri.split("\\.");
			if (parts.length > 0) {
				var lastPart = parts[parts.length - 1];
				var ext = lastPart.toLowerCase();
				Assert.isTrue(ext.length() <= 4, "the extension must be no larger than 4 characters");
				return contextualFile(podcast, "image." + ext);
			}
		}
		throw new IllegalArgumentException(
				"we could not determine the image file for the podcast, " + podcast.getGuid());
	}

	private File contextualRootFolderFor(SoundcloudPodcast podcast) {
		return new File(this.root, buildUniqueFileNameForPodcast(podcast));
	}

	private File contextualFile(SoundcloudPodcast podcast, String fileName) {
		return new File(contextualRootFolderFor(podcast), fileName);
	}

	@SneakyThrows
	private Collection<SoundcloudPodcast> readPodcastsFromRssFeed() {
		var urlResource = new UrlResource(uri);
		var dbf = DocumentBuilderFactory.newInstance();
		var documentBuilder = dbf.newDocumentBuilder();
		var podcasts = new ArrayList<SoundcloudPodcast>();
		try (var inputStream = urlResource.getInputStream()) {
			var doc = documentBuilder.parse(inputStream);
			NodeList items = doc.getElementsByTagName("item");
			for (var i = 0; i < items.getLength(); i++) {
				var currentItem = items.item(i);
				podcasts.add(buildFromItem((Element) currentItem));
			}
		}
		return podcasts;
	}

	@SneakyThrows
	private File download(URI url, File file) {
		ensureDirectoryExists(file.getParentFile());
		Assert.isTrue(file.getParentFile().exists() || file.getParentFile().mkdirs(),
				"we couldn't create the parent file, " + file.getParentFile().getAbsolutePath());
		if (file.exists() && file.length() > 0) {
			log.warn("the file " + file.getAbsolutePath() + " already exists. Skipping download.");
			return file;
		}
		var resource = this.restTemplate.getForEntity(url, Resource.class);
		Assert.isTrue(resource.getStatusCode().is2xxSuccessful(),
				"the response status for the URI " + url.toString() + " is invalid");
		try (var in = new BufferedInputStream(Objects.requireNonNull(resource.getBody()).getInputStream());
				var out = new BufferedOutputStream(new FileOutputStream(file))) {
			FileCopyUtils.copy(in, out);
		}
		Assert.isTrue(file.exists() && file.length() > 0,
				"we could not download the URI at " + url.toString() + " to " + file.getAbsolutePath() + ".");
		return file;
	}

	private void ensureDirectoryExists(File file) {
		Assert.isTrue(file.exists() || file.mkdirs(),
				"the directory '" + file.getAbsolutePath() + "' does not exist and could not be created.");
	}

	@SneakyThrows
	private void writeJsonFor(SoundcloudPodcast podcast) {
		var json = this.objectMapper.writer().writeValueAsString(podcast);
		var jsonFile = this.jsonFileFor(podcast);
		ensureDirectoryExists(jsonFile.getParentFile());
		FileCopyUtils.copy(json, new FileWriter(jsonFile));
		Assert.isTrue(jsonFile.exists(), "the .json file for " + podcast.getGuid() + " could not be written");
		log.info("downloaded json descriptor " + jsonFile.getAbsolutePath());
	}

	@SneakyThrows
	private void doDownload(SoundcloudPodcast podcast) {

		this.writeJsonFor(podcast);

		var mp3File = this.download(podcast.getMp3Uri(), this.mp3FileFor(podcast));
		log.info("downloaded audio " + mp3File.getAbsolutePath());

		var imageFile = this.download(podcast.getImageUri(), this.imageFileFor(podcast));
		log.info("downloaded image " + imageFile.getAbsolutePath());
	}

	private File jsonFileFor(SoundcloudPodcast podcast) {
		return contextualFile(podcast, "podcast.json");
	}

	@SneakyThrows
	@EventListener(ApplicationReadyEvent.class)
	public void downloadAllAssetsFromSoundcloud() {
		this.readPodcastsFromRssFeed().forEach(this::doDownload);
	}

	@SneakyThrows
	private SoundcloudPodcast buildFromItem(Element node) {
		var guid = node.getElementsByTagName("guid").item(0).getTextContent();
		var title = node.getElementsByTagName("title").item(0).getTextContent();
		var pubDate = this.dateFormat.parse(node.getElementsByTagName("pubDate").item(0).getTextContent());
		var linkToSoundcloudEpisode = URI.create(node.getElementsByTagName("link").item(0).getTextContent());
		var description = node.getElementsByTagName("description").item(0).getTextContent();
		var mp3EnclosureNode = node.getElementsByTagName("enclosure").item(0);
		var mp3EnclosureType = XmlUtils.getAttribute(mp3EnclosureNode, "type");
		Assert.isTrue(mp3EnclosureType.equalsIgnoreCase("audio/mpeg"), "the media file needs to be an .mp3");
		var mp3EnclosureUri = URI.create(XmlUtils.getAttribute(mp3EnclosureNode, "url"));
		Assert.notNull(mp3EnclosureUri, "you must provide a valid .mp3 URI from SoundCloud");
		var itunesImageNode = node.getElementsByTagName("itunes:image").item(0);
		var itunesImageNodeUri = URI.create(XmlUtils.getAttribute(itunesImageNode, "href"));
		return new SoundcloudPodcast(guid, title, pubDate, linkToSoundcloudEpisode, description, mp3EnclosureUri,
				itunesImageNodeUri);
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