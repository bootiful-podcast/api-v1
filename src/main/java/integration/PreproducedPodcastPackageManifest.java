package integration;

import lombok.Data;

@Data
public class PreproducedPodcastPackageManifest extends PodcastPackageManifest {

	protected ProducedAudio producedAudio = new ProducedAudio();

	public static PreproducedPodcastPackageManifest from(String uid, String title, String description,
			String producedAudioFileName, String photoFileName) {
		var pm = new PreproducedPodcastPackageManifest();
		pm.description = description;
		pm.title = title;
		pm.uid = uid;
		pm.getPhoto().setSrc(photoFileName);
		pm.getProducedAudio().setSrc(producedAudioFileName);
		return pm;
	}

}
