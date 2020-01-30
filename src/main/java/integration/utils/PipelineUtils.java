package integration.utils;

import integration.database.Podcast;
import lombok.SneakyThrows;

import java.util.Optional;

public abstract class PipelineUtils {

	@SneakyThrows
	public static Podcast podcastOrElseThrow(String uid, Optional<Podcast> optionalPodcast) {
		return optionalPodcast.orElseThrow(() -> podcastNotFoundException(uid));
	}

	public static Exception podcastNotFoundException(String uid) {
		return new IllegalArgumentException(podcastNotFoundErrorMessage(uid));
	}

	public static String podcastNotFoundErrorMessage(String uid) {
		return "no podcast with UID " + uid + " was found!";
	}

}
