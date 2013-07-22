package eu.trentorise.smartcampus.vas.experiencebuster.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import eu.trentorise.smartcampus.eb.model.Content;
import eu.trentorise.smartcampus.eb.model.ContentType;
import eu.trentorise.smartcampus.eb.model.Experience;
import eu.trentorise.smartcampus.eb.model.ModeratedContent;
import eu.trentorise.smartcampus.filestorage.client.BasicAuthentication;
import eu.trentorise.smartcampus.filestorage.client.Filestorage;
import eu.trentorise.smartcampus.filestorage.client.FilestorageException;
import eu.trentorise.smartcampus.filestorage.client.model.Token;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.ExperienceManager;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.ModeratedContentManager;

@Controller
public class ModeratedContentController {

	private static final Logger logger = Logger
			.getLogger(ModeratedContentController.class);

	private static final long expiration = 4 * 60 * 60 * 1000;
	@Autowired
	ModeratedContentManager contentManager;

	@Autowired
	ExperienceManager expManager;

	@Autowired
	Filestorage filestorage;

	@Autowired
	BasicAuthentication authentication;

	private static Multimap<Long, ApprovedContent> approvedContents = Multimaps
			.synchronizedListMultimap(ArrayListMultimap
					.<Long, ApprovedContent> create());

	@RequestMapping(method = RequestMethod.GET, value = "/approvedcontent/{entityId}")
	public @ResponseBody
	Collection<String> getApprovedContents(@PathVariable long entityId) {

		List<String> urls = new ArrayList<String>();

		Collection<ApprovedContent> contents = approvedContents.get(entityId);
		for (ApprovedContent content : contents) {
			urls.add(content.getUrl());
		}
		return urls;
	}

	// in ms
	// delay 1min
	@Scheduled(fixedDelay = 60000)
	public void update() {
		List<ModeratedContent> contents = contentManager.getModeratedContents(
				true, true);
		for (ModeratedContent content : contents) {

			List<Experience> exps = expManager.getByContent(ContentType.PHOTO,
					content.getName());
			if (!exps.isEmpty()) {
				logger.info(String.format("Content present in experience %s",
						exps.get(0).getId()));
				Content nearMeObject = exps.get(0).getObject();
				if (nearMeObject != null && nearMeObject.getEntityId() > 0) {
					Content photo = expManager.getContentBy(exps.get(0),
							ContentType.PHOTO, content.getName());
					if (photo != null && photo.getValue() != null) {
						try {

							// Token photoToken = filestorage
							// .getPublicResourceToken(authentication,
							// photo.getValue());
							Token photoToken = filestorage
									.getMobileResourceToken(authentication,
											photo.getValue());
							approvedContents.put(
									nearMeObject.getEntityId(),
									new ApprovedContent(photoToken.getUrl(),
											System.currentTimeMillis()
													+ expiration, photo
													.getValue()));
						} catch (FilestorageException e) {
							logger.error("Exception retrieving public resource token: "
									+ photo.getValue());
						}
					} else {
						logger.warn(String
								.format("No content found with name %s, or value note present",
										content.getName()));
					}

				} else {
					logger.warn(String.format(
							"NearMeObject doesn't exist for experience %s",
							exps.get(0).getId()));
				}
			} else {
				logger.warn(String.format("%s doesn't exist", content.getName()));
			}
		}
	}

	// every 5min
	@Scheduled(fixedRate = 300000)
	public void refreshUrls() {
		Collection<Entry<Long, ApprovedContent>> contents = approvedContents
				.entries();

		for (Entry<Long, ApprovedContent> entry : contents) {
			ApprovedContent content = entry.getValue();
			if (content.getExpirationUrlTime() <= System.currentTimeMillis()) {
				try {
					// Token resourceToken = filestorage.getPublicResourceToken(
					// authentication, content.getResourceId());
					Token resourceToken = filestorage.getMobileResourceToken(
							authentication, content.getResourceId());
					content.setUrl(resourceToken.getUrl());
					content.setExpirationUrlTime(System.currentTimeMillis()
							+ expiration);
				} catch (FilestorageException e) {
					logger.error(String.format(
							"Exception refreshing url of resource %s",
							content.getResourceId()));
				}
			}
		}
	}
}

class ApprovedContent {
	private String url;
	private long expirationUrlTime;
	private String resourceId;

	public ApprovedContent() {
	}

	public ApprovedContent(String url, long expirationUrlTime, String resourceId) {
		super();
		this.url = url;
		this.expirationUrlTime = expirationUrlTime;
		this.resourceId = resourceId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getExpirationUrlTime() {
		return expirationUrlTime;
	}

	public void setExpirationUrlTime(long expirationUrlTime) {
		this.expirationUrlTime = expirationUrlTime;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

}
