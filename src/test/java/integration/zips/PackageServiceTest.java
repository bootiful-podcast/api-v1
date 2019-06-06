package integration.zips;

import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.File;

@Log4j2
class PackageServiceTest {

	private final File root = new File(System.getProperty("user.home"), "Desktop/zips");
	private final PackageService packageService = new PackageService(this.root);

	PackageServiceTest() {
		Assert.assertTrue("the directory " + this.root.getAbsolutePath() +
			" does not exist", this.root.exists() || this.root.mkdirs());
	}

	@Test
	void createPackage() throws Exception {

		var intro = new File("/Users/joshlong/Desktop/pipeline/input/oleg-intro.wav");
		var interview = new File("/Users/joshlong/Desktop/pipeline/input/oleg-interview.wav");
		var result = this.packageService.createPackage(intro, interview);
		log.info("ding! the result is available at " + result.getAbsolutePath() + ".");
	}
}