package integration.zips;

import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class PackageService {

	public File createPackage(File intro, File interview) throws Exception {
		var result = File.createTempFile("package", ".zip");
		var srcFiles = Arrays.asList(intro, interview);
		try (var outputStream = new BufferedOutputStream(new FileOutputStream(result));
							var zipOutputStream = new ZipOutputStream(outputStream)) {
			for (var fileToZip : srcFiles) {
				try (var inputStream = new BufferedInputStream(new FileInputStream(fileToZip))) {
					var zipEntry = new ZipEntry(fileToZip.getName());
					zipOutputStream.putNextEntry(zipEntry);
					StreamUtils.copy(inputStream, zipOutputStream);
				}
			}
		}
		return result;
	}
}
