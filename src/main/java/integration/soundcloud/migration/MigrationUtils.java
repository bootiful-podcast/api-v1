package integration.soundcloud.migration;

import java.io.File;

abstract class MigrationUtils {

	private static File root = new File(new File(System.getProperty("user.home"), "Desktop"), "soundcloud");

	public static File getRoot() {
		return root;
	}

}
