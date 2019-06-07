package integration.zips;

import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

@Log4j2
class PackageServiceTest {

	private final File root = new File(System.getProperty("user.home"), "Desktop");

	private final PackageService packageService = new PackageService();

	PackageServiceTest() {
		Assert.assertTrue(
				"the directory " + this.root.getAbsolutePath() + " does not exist",
				this.root.exists() || this.root.mkdirs());
	}

	@Test
	void xmlFor() {
		var input = new File(this.root, "input");

		var mp3Intro = new File(input, "intro.mp3");
		var mp3Interview = new File(input, "interview.mp3");

		var wavIntro = new File(input, "intro.wav");
		var wavInterview = new File(input, "interview.wav");

		var xml = this.packageService.xmlFor("Spring Cloud lead Spencer Gibb",
				UUID.randomUUID().toString(),
				new PackageService.Media("wav", wavIntro, wavInterview),
				new PackageService.Media("mp3", mp3Intro, mp3Interview));

		Assert.assertTrue(xml.contains("interview=\"interview.wav\""));
		Assert.assertTrue(xml.contains("interview=\"interview.mp3\""));
		Assert.assertTrue(xml.contains("intro=\"intro.mp3\""));
		Assert.assertTrue(xml.contains("intro=\"intro.wav\""));
	}

	@Test
	void createPackage() throws Exception {

		var intro = new File("/Users/joshlong/Desktop/pipeline/input/oleg-intro.wav");
		var interview = new File(
				"/Users/joshlong/Desktop/pipeline/input/oleg-interview.wav");
		var result = this.packageService.createPackage("description",
				UUID.randomUUID().toString(), null,
				new PackageService.Media("wav", intro, interview));
		log.info("ding! the result is available at " + result.getAbsolutePath());
	}

}