package integration.utils;

import lombok.SneakyThrows;
import org.springframework.util.Assert;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class UnzipUtils {

	@SneakyThrows
	public static Collection<File> unzip(File zipfile, File targetDirectory) {
		var list = new ArrayList<File>();
		var buffer = new byte[1024];
		try (var zis = new ZipInputStream(new FileInputStream(zipfile))) {
			ZipEntry zipEntry;
			while ((zipEntry = zis.getNextEntry()) != null) {
				var newFile = newFile(targetDirectory, zipEntry);
				try (var fos = new BufferedOutputStream(new FileOutputStream(newFile))) {
					var len = 0;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
				}
				list.add(newFile);
			}
			zis.closeEntry();
		}
		return list;
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
