package integration;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

/**
 * The client will soon start producing the manifest entirely in browser-memory and in
 * JavaScript and I want to make sure that the parser on the server-side knows what to do
 * with the XML it's given from the client.
 */
@Log4j2
public class PodcastManifestInteroperabilityTest {

	private File buildFileFromFileName(String fn) {
		var home = System.getenv("HOME");
		return new File(home + "/Desktop/" + fn);
	}

	@Test
	public void manifest() throws Exception {

		var xmlFromJavaScript = "    <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><podcast title=\"the Title\" uid=\"13232-224232-4353-21231\">\n"
				+ "    <interview src=\"interview.mp3\"/>\n" + "    <introduction src=\"intro.mp3\"/>\n"
				+ "    <photo src=\"photo.jpg\"/>\n"
				+ "    <description>This is the potentially very long multine XML-riddled and HTML-riddled description of the show. &lt;em&gt;I &lt;B&gt;love&lt;/B&gt;&lt;/em&gt; this show!</description>\n"
				+ "    </podcast>";

		var xmlFromJava = buildXmlManifestForPackage("the Title",
				"This is the potentially very long multine XML-riddled and HTML-riddled description of the show. <em>I <B>love</B></em> this show!"
						.trim(),
				"13232-224232-4353-21231", //
				buildFileFromFileName("intro.mp3"), //
				buildFileFromFileName("interview.mp3"), //
				buildFileFromFileName("photo.jpg"));

		var js = from(xmlFromJavaScript);
		var java = from(xmlFromJava);
		Assertions.assertEquals(js.getInterview().getSrc(), java.getInterview().getSrc());
		Assertions.assertEquals(js.getIntroduction().getSrc(), java.getIntroduction().getSrc());
		Assertions.assertEquals(js.getPhoto().getSrc(), java.getPhoto().getSrc());
		Assertions.assertEquals(js.getTitle(), java.getTitle());
		Assertions.assertEquals(js.getDescription(), java.getDescription());
	}

	private UnproducedPodcastPackageManifest from(String xml) {
		return UnproducedPodcastPackageManifest.from(new ByteArrayInputStream(xml.getBytes()));
	}

	@SneakyThrows
	private static String buildXmlManifestForPackage(String title, String description, String uid, File intro,
			File interview, File photo) {

		var docFactory = DocumentBuilderFactory.newInstance();
		var docBuilder = docFactory.newDocumentBuilder();
		var doc = docBuilder.newDocument();
		var rootElement = doc.createElement("podcast");
		rootElement.setAttribute("uid", uid);
		rootElement.setAttribute("title", title);
		doc.appendChild(rootElement);
		var interviewElement = createElementWithAttributes(doc, "interview", Map.of("src", interview.getName()));
		var introductionElement = createElementWithAttributes(doc, "introduction", Map.of("src", intro.getName()));
		var photoElement = createElementWithAttributes(doc, "photo", Map.of("src", photo.getName()));
		var descriptionElement = doc.createElement("description");
		descriptionElement.appendChild(doc.createCDATASection(description));
		Arrays.asList(interviewElement, introductionElement, photoElement, descriptionElement)
				.forEach(rootElement::appendChild);
		var transformerFactory = TransformerFactory.newInstance();
		var transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		var source = new DOMSource(doc);
		var stringWriter = new StringWriter();
		var result = new StreamResult(stringWriter);
		transformer.transform(source, result);
		return stringWriter.toString();
	}

	private static Element createElementWithAttributes(Document doc, String elementName, Map<String, String> attrs) {
		var element = doc.createElement(elementName);
		attrs.forEach(element::setAttribute);
		return element;
	}

}
