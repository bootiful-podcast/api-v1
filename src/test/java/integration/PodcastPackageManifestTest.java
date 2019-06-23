package integration;

import org.apache.commons.io.Charsets;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;

class PodcastPackageManifestTest {

	String interview = "oleg-interview.mp3";

	String intro = "oleg-intro.mp3";

	String description = "Josh talks to some people.";

	String uid = "1143a526-3d70-41ea-8ec5-974a1a0b319f";

	String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
			+ "<podcast description=\"" + this.description + "\" uid=\"" + this.uid
			+ "\">\n" + "  <mp3 interview=\"" + this.interview + "\" intro=\""
			+ this.intro + "\"/>\n" + "</podcast>\n";

	Charset charset = Charsets.toCharset("UTF-8");

	@Test
	public void fromFile() throws Exception {
		var tempFile = File.createTempFile("temp", "xml");
		tempFile.deleteOnExit();
		try (var out = new FileOutputStream(tempFile)) {
			StreamUtils.copy(this.xml, this.charset, out);
		}
	}

	@Test
	public void fromString() {
		var manifest = PodcastPackageManifest.from(xml);
		Assert.assertEquals("the UID must match", manifest.getUid(), uid);
		Assert.assertEquals("the description must match", manifest.getDescription(),
				description);
		Assert.assertFalse("the media collection must be non-null",
				manifest.getMedia().isEmpty());
		Iterator<PodcastPackageManifest.Media> iterator = manifest.getMedia().iterator();
		PodcastPackageManifest.Media next = iterator.next();
		Assert.assertEquals("the interview must match", next.getInterview(), interview);
		Assert.assertEquals("the intro must match", next.getIntroduction(), intro);
	}

}