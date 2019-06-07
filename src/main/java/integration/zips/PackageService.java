package integration.zips;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
public class PackageService {

	private static void addElementFor(Document doc, Element root, String elementName,
			Map<String, String> attrs) {
		Element element = doc.createElement(elementName);
		attrs.forEach(element::setAttribute);
		root.appendChild(element);
	}

	@Data
	public static class Media {

		private final String format;

		private final File intro, interview;

		Media(String format, File intro, File interview) {
			Assert.notNull(format, "the format must not be null");
			Assert.notNull(interview, "the interview file must not be null");
			Assert.notNull(intro, "the intro file must not be null");
			this.format = format;
			this.intro = intro;
			this.interview = interview;
		}

	}

	private static void addAttributesForMedia(Document doc, Element root, Media media) {
		if (null == media) {
			return;
		}
		var intro = media.getIntro();
		var interview = media.getInterview();
		var attrs = Map.of("intro", intro.getName(), "interview", interview.getName());
		addElementFor(doc, root, media.getFormat(), attrs);
	}

	@SneakyThrows
	String xmlFor(String description, String uid, Media mp3, Media wav) {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		var doc = docBuilder.newDocument();
		var rootElement = doc.createElement("podcast");
		rootElement.setAttribute("description", description);
		rootElement.setAttribute("uid", uid);
		doc.appendChild(rootElement);

		addAttributesForMedia(doc, rootElement, mp3);
		addAttributesForMedia(doc, rootElement, wav);

		var transformerFactory = TransformerFactory.newInstance();

		var transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		// transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		var source = new DOMSource(doc);
		var stringWriter = new StringWriter();
		var result = new StreamResult(stringWriter);
		transformer.transform(source, result);
		return stringWriter.toString();
	}

	private static void expand(Media m, Collection<File> files) {
		if (null == m)
			return;
		files.add(m.getInterview());
		files.add(m.getIntro());
	}

	public File createPackage(String description, String uid, Media mp3, Media wav)
			throws Exception {

		var staging = Files.createTempDirectory("staging").toFile();

		var xmlFile = new File(staging, "manifest.xml");
		try (var xmlOutputStream = new BufferedWriter(new FileWriter(xmlFile))) {
			var xml = this.xmlFor(description, uid, mp3, wav);
			FileCopyUtils.copy(xml, xmlOutputStream);
			log.info("wrote " + xmlFile.getAbsolutePath() + " with content " + xml);
		}

		// zip
		var zipFile = new File(staging, UUID.randomUUID().toString() + ".zip");

		var srcFiles = new ArrayList<File>();
		srcFiles.add(xmlFile);
		expand(mp3, srcFiles);
		expand(wav, srcFiles);

		try (var outputStream = new BufferedOutputStream(new FileOutputStream(zipFile));
				var zipOutputStream = new ZipOutputStream(outputStream)) {
			for (var fileToZip : srcFiles) {
				try (var inputStream = new BufferedInputStream(
						new FileInputStream(fileToZip))) {
					var zipEntry = new ZipEntry(fileToZip.getName());
					zipOutputStream.putNextEntry(zipEntry);
					StreamUtils.copy(inputStream, zipOutputStream);
				}
			}
		}
		return zipFile;
	}

}
