package integration.soundcloud.migration;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.Date;

@Data
@RequiredArgsConstructor
class SoundcloudPodcast {

	private final String guid, title;

	private final Date pubDate;

	private final URI linkToSoundcloudEpisode;

	private final String description;

	private final URI mp3Uri, imageUri;

}
