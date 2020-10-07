package integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Data
@AllArgsConstructor
@Log4j2
@NoArgsConstructor
public class PodcastPackageManifest {

	protected String title, description, uid;

	protected Photo photo = new Photo();

	@Data
	public static class Interview {

		private String src = "";

	}

	@Data
	public static class Introduction {

		private String src = "";

	}

	@Data
	public static class Photo {

		private String src = "";

	}

	@Data
	public static class ProducedAudio {

		private String src = "";

	}

}