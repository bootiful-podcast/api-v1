package integration.upload;


import lombok.SneakyThrows;
import org.springframework.util.Assert;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


class Unzipper {



	private final File root;

	Unzipper(File root) {
		this.root = root;
	}

	Collection<File> unzip(File zipfile) {
		var list = new ArrayList<File>();
		var localRoot = new File(this.root, UUID.randomUUID().toString());
		Assert.isTrue(localRoot.exists() || localRoot.mkdirs(), "The " + localRoot.getAbsolutePath() + " does not exist and couldn't be created");
		unzip(zipfile, localRoot, list);
		return list;
	}

	@SneakyThrows
	private static void unzip(File fileZip, File destDir, Collection<File> files) {
		var buffer = new byte[1024];
		try (var zis = new ZipInputStream(new FileInputStream(fileZip))) {
			ZipEntry zipEntry;
			while ((zipEntry = zis.getNextEntry()) != null) {
				var newFile = newFile(destDir, zipEntry);
				try (var fos = new BufferedOutputStream(new FileOutputStream(newFile))) {
					var len = 0;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
				}
				files.add(newFile);
			}
			zis.closeEntry();
		}
	}

	@SneakyThrows
	private static File newFile(File destinationDir, ZipEntry zipEntry) {
		var destFile = new File(destinationDir, zipEntry.getName());
		var destDirPath = destinationDir.getCanonicalPath();
		var destFilePath = destFile.getCanonicalPath();
		var entryIsInTargetDirectory = destFilePath.startsWith(destDirPath + File.separator);
		Assert.isTrue(entryIsInTargetDirectory, "Entry is outside of the target dir: " + zipEntry.getName());
		return destFile;
	}
}
