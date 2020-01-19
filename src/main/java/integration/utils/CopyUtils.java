package integration.utils;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import java.io.*;

@Log4j2
public abstract class CopyUtils {

	@SneakyThrows
	public static void copy(InputStream in, OutputStream out) {
		try (in; out) {
			FileCopyUtils.copy(in, out);
		}
	}

	@SneakyThrows
	public static void copy(InputStream in, File out) {
		try (var fin = in instanceof BufferedInputStream ? in
				: new BufferedInputStream(in);
				var fout = new BufferedOutputStream(new FileOutputStream(out))) {
			copy(fin, fout);
		}
	}

	@SneakyThrows
	public static void copy(File in, OutputStream out) {
		try (var fin = new BufferedInputStream(new FileInputStream(in));
				var fout = out instanceof BufferedOutputStream ? out
						: new BufferedOutputStream(out)) {
			copy(fin, fout);
		}
	}

	public static String extensionFor(String fileName) {
		var lastIndexOf = fileName.lastIndexOf(".");
		var trim = fileName.substring(lastIndexOf).toLowerCase().trim();
		if (trim.startsWith(".")) {
			return trim.substring(1);
		}
		return trim;
	}

	public static String extensionFor(File file) {
		var name = file.getName();
		return extensionFor(name);
	}

	@SneakyThrows
	private static void copyDirectory(File og, File target) {
		Assert.isTrue(!target.exists() || FileSystemUtils.deleteRecursively(target),
				"the target directory " + target.getAbsolutePath()
						+ " exists and could not be deleted");
		FileSystemUtils.copyRecursively(og, target);
	}

	@SneakyThrows
	private static void copyFile(File og, File target) {
		Assert.isTrue((target.exists() && target.delete()) || !target.exists(),
				"the target file " + target.getAbsolutePath()
						+ " exists, but could not be deleted");
		FileCopyUtils.copy(og, target);
	}

	@SneakyThrows
	public static File copy(File og, File target) {
		log.info("copying from " + og.getAbsolutePath() + " to "
				+ target.getAbsolutePath());
		if (og.isFile()) {
			copyFile(og, target);
		}
		else if (og.isDirectory()) {
			copyDirectory(og, target);
		}
		return target;
	}

	public static boolean delete(File f) {
		if (!f.exists()) {
			return true;
		}
		if (f.isFile()) {
			return f.delete();
		}
		else {
			return FileSystemUtils.deleteRecursively(f);
		}
	}

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
		var allContents = f.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectoryRecursively(file);
			}
		}
		return f.delete();
	}

}
