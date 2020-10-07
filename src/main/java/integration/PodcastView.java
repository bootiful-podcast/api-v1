package integration;

import integration.database.Link;
import integration.database.Media;
import integration.database.Podcast;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Representation of a {@code Podcast} for clients
 */
public class PodcastView extends Podcast {

	public PodcastView(Long id, String uid, String title, String description, String notes, String transcript,
			String s3AudioUri, String s3PhotoUri, String s3AudioFileName, String s3PhotoFileName,
			Date podbeanDraftCreated, Date podbeanDraftPublished, String podbeanMediaUri, String podbeanPhotoUri,
			Date date, List<Link> links, List<Media> media) {
		super(id, uid, title, description, notes, transcript, s3AudioUri, s3PhotoUri, s3AudioFileName, s3PhotoFileName,
				podbeanDraftCreated, podbeanDraftPublished, podbeanMediaUri, podbeanPhotoUri, date, links, media);
	}

	public static PodcastView from(Podcast p) {
		return new PodcastView(p.getId(), p.getUid(), p.getTitle(), p.getDescription(), "", "", p.getS3AudioUri(),
				p.getS3PhotoUri(), p.getS3AudioFileName(), p.getS3PhotoFileName(), p.getPodbeanDraftCreated(),
				p.getPodbeanDraftPublished(), p.getPodbeanMediaUri(), p.getPodbeanPhotoUri(), p.getDate(),
				Collections.emptyList(), Collections.emptyList());
	}

}
