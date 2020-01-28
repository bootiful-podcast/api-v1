package integration;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.File;

@Data
@RequiredArgsConstructor
public class ProducedPodcast {

	private final String uid;

	private final String title;

	private final String description;

	private final File producedAudio;

	private final File episodePhoto;

}
