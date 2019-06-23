package integration.utils;

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

	public static boolean deleteDirectoryRecursively(File f) {
		Assert.notNull(f, "the file specified for deletion must be non-null");
		File[] allContents = f.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectoryRecursively(file);
			}
		}
		return f.delete();
	}

}
