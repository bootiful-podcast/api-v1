package pl;

import org.springframework.util.Assert;

import java.io.File;

public abstract class FileUtils {

	public static void assertFileExists(File f) {
		Assert.notNull(f, "you must provide a non-null argument");
		Assert.isTrue(f.exists(), "the file should exist at " + f.getAbsolutePath());
	}

	public static File ensureDirectoryExists(File f) {
		Assert.notNull(f, "you must provide a non-null argument");
		Assert.isTrue(f.exists() || f.mkdirs(),
				"the file " + f.getAbsolutePath() + " does not exist");
		return f;
	}

}
