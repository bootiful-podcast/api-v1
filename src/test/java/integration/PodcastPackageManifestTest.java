package integration;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class PodcastPackageManifestTest {

	private final ClassPathResource classPathResource = new ClassPathResource(
			"/manifest.xml");

	@Test
	public void factory() throws Exception {

		PodcastPackageManifest sample = PodcastPackageManifest
				.from(this.classPathResource.getInputStream());
		Assert.assertEquals(sample.getTitle(), "A Title");
		Assert.assertEquals(sample.getUid(), "eeb3a612-928e-40a2-b238-a59c05ee4b4a");
		Assert.assertEquals(sample.getInterview().getSrc(), "interview.mp3");
		Assert.assertEquals(sample.getIntroduction().getSrc(), "introduction.mp3");
		Assert.assertEquals(sample.getPhoto().getSrc(), "dave-and-i.jpg");
		Assert.assertEquals(sample.getDescription(), "Description 123");
	}

}
