/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package eu.trentorise.smartcampus.vas.experiencebuster.manager;

import it.unitn.disi.sweb.webapi.client.WebApiException;
import it.unitn.disi.sweb.webapi.client.smartcampus.SCWebApiClient;
import it.unitn.disi.sweb.webapi.model.entity.Entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.ac.provider.model.User;
import eu.trentorise.smartcampus.common.SemanticHelper;
import eu.trentorise.smartcampus.eb.model.Content;
import eu.trentorise.smartcampus.eb.model.ContentType;
import eu.trentorise.smartcampus.eb.model.ExpCollection;
import eu.trentorise.smartcampus.eb.model.Experience;
import eu.trentorise.smartcampus.eb.model.UserPreference;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.data.BasicObject;
import eu.trentorise.smartcampus.vas.experiencebuster.filter.ExperienceFilter;
import eu.trentorise.smartcampus.vas.experiencebuster.storage.ExperienceStorage;

@Component
public class ExperienceManager {

	private static final Logger logger = Logger
			.getLogger(ExperienceManager.class);

	@Autowired
	ExperienceStorage storage;

	@Autowired
	PreferenceManager prefManager;

	@Autowired
	FileManager fileManager;

	SCWebApiClient socialClient;

	@Value("${smartcampus.vas.web.socialengine.host}")
	private String socialHost;
	@Value("${smartcampus.vas.web.socialengine.port}")
	private int socialPort;

	public ExperienceManager() throws IOException {
	}

	@PostConstruct
	private void init() {
		socialClient = SCWebApiClient.getInstance(Locale.ENGLISH, socialHost,
				socialPort);
	}

	public List<Experience> getAll(User user) throws NotFoundException,
			DataException {
		return storage.getObjectsByType(Experience.class, "" + user.getId());
	}

	public Experience getById(String id) throws NotFoundException,
			DataException {
		return storage.getObjectById(id, Experience.class);
	}

	public Content storeContent(Experience exp, Content content, byte[] file)
			throws ExperienceBusterException {

		storage.processStoringContent(content);

		if (isUploadableContent(content) && file != null) {
			Long fid = fileManager.updload(exp.getSocialUserId(), file);
			content.setValue(fid.toString());
		}

		createSocialEntity(content, exp.getSocialUserId());
		return content;

	}

	public Experience store(User user, Experience exp, byte[] file)
			throws DataException {
		if (exp.getContents() == null || exp.getContents().size() != 1) {
			throw new DataException(
					"Experience must contain only a content during creation");
		}
		exp.setUser("" + user.getId());
		exp.setSocialUserId(user.getSocialId());
		exp.setId(new ObjectId().toString());
		exp.setCreationTime(System.currentTimeMillis());

		if (!checkCollectionConsistency(exp)) {
			throw new DataException(
					"Experience refers to a nonexistent collection for the user");
		}
		storage.processStoringContent(exp.getContents());
		try {
			// create experience social entity and if necessary content social
			// entities
			createSocialEntity(exp);

			storeContent(exp, exp.getContents().get(0), file);
			updateEntityRelations(exp);
			storage.storeObject(exp);
		} catch (ExperienceBusterException e) {
			logger.error("Exception storing experience");
		}
		return exp;
	}

	public <T extends BasicObject> void update(Experience experience)
			throws NotFoundException, DataException, ExperienceBusterException {
		Experience saved = getById(experience.getId());
		if (!checkCollectionConsistency(experience)) {
			throw new DataException(
					"Experience refers to a nonexistent collection for the user");
		}
		// update only basic information, no contents
		updateExperience(experience, saved);
		storage.updateObject(saved);
	}

	public void removeExperience(String id) throws NotFoundException,
			DataException, ExperienceBusterException {
		Experience exp = storage.getObjectById(id, Experience.class);
		for (Content content : exp.getContents()) {
			deleteSocialEntity(content);
			if (isUploadableContent(content) && content.getValue() != null) {
				fileManager.delete(new Long(content.getValue()));
			}
		}
		deleteSocialEntity(exp);
		storage.deleteObject(exp);
	}

	public boolean checkPermission(String eid, String cid, User user,
			Permission permission) throws NotFoundException, DataException {
		Experience object = storage.getObjectById(eid, Experience.class);
		boolean result = false;
		switch (permission) {
		case UPDATE:
			result = object.getSocialUserId() == user.getSocialId()
					&& checkContent(object, cid);
			break;
		default:
			throw new UnsupportedOperationException();
		}

		return result;
	}

	public boolean checkPermission(String eid, User user, Permission permission)
			throws NotFoundException, DataException,
			UnsupportedOperationException, ExperienceBusterException {
		Experience object = storage.getObjectById(eid, Experience.class);
		boolean result = false;
		switch (permission) {
		case UPDATE:
		case DELETE:
			result = object.getUser().equals(Utils.userId(user));
			break;
		case READ:
			result = object.getUser().equals(Utils.userId(user));
			if (!result) {
				try {
					result = SemanticHelper.isEntitySharedWithUser(
							socialClient, object.getEntityId(),
							user.getSocialId());
				} catch (WebApiException e) {
					throw new ExperienceBusterException();
				}
			}
		default:
			throw new UnsupportedOperationException();
		}

		return result;
	}

	private void updateEntityRelations(Experience exp)
			throws ExperienceBusterException {
		List<Long> entityIds = new ArrayList<Long>();
		for (Content c : exp.getContents()) {
			if (c.getEntityId() > 0) {
				entityIds.add(c.getEntityId());
			}
		}
		try {
			SemanticHelper.updateEntity(socialClient, exp.getEntityId(), null,
					null, null, entityIds.isEmpty() ? null : entityIds);
		} catch (WebApiException e) {
			throw new ExperienceBusterException();
		}
	}

	private void updateExperience(Experience source, Experience target)
			throws ExperienceBusterException {
		target.setAddress(source.getAddress());
		target.setDescription(source.getDescription());
		target.setLocation(source.getLocation());
		target.setCollectionIds(source.getCollectionIds());
		target.setEntityId(source.getEntityId());
		target.setSocialUserId(source.getSocialUserId());
		target.setTags(source.getTags());
		target.setTitle(source.getTitle());

		try {
			SemanticHelper.updateEntity(socialClient, source.getEntityId(),
					source.getTitle(), source.getDescription(),
					source.getTags(), null);
		} catch (WebApiException e) {
			logger.error("Exception updating social entity of experience: social:"
					+ source.getEntityId() + " exp: " + source.getId());
			throw new ExperienceBusterException();
		}

	}

	public void updateContent(String expId, Content content)
			throws DataException, NotFoundException, ExperienceBusterException {
		if (content.getId() == null) {
			throw new DataException("Content without id");
		}
		Experience exp = getById(expId);
		boolean founded = false;
		for (Content c : exp.getContents()) {
			if ((founded = c.getId().equals(content.getId()))) {
				c.setNote(content.getNote());
				c.setTimestamp(System.currentTimeMillis());
				update(exp);
				break;
			}
		}
		if (!founded) {
			logger.error("Content to update not present in experience "
					+ content.getId());
			throw new NotFoundException("Content not present");
		}

	}

	public void addContent(String expId, Content content, byte[] file)
			throws NotFoundException, DataException, ExperienceBusterException {
		Experience exp = getById(expId);
		storage.processStoringContent(content);

		if (exp.getContents().contains(content)) {
			throw new DataException("Content already present in Experience: "
					+ content.getId());
		}
		storeContent(exp, content, file);
		exp.getContents().add(content);
		exp.setContents(exp.getContents());

		try {
			updateEntityRelations(exp);
		} catch (ExperienceBusterException e) {
			logger.error("Exception updating experience entity relations, expId: "
					+ exp.getId());
			throw e;
		}
		storage.updateObject(exp);
	}

	public void removeContent(String expId, Content content)
			throws NotFoundException, DataException, ExperienceBusterException {
		Experience exp = getById(expId);
		if (exp.getContents().size() == 1) {
			throw new DataException("Experience must have almost one content");
		}
		exp.getContents().remove(content);
		exp.setContents(exp.getContents());
		try {
			deleteSocialEntity(content);
			if (isUploadableContent(content) && content.getValue() != null) {
				fileManager.delete(new Long(content.getValue()));
			}

			updateEntityRelations(exp);
		} catch (ExperienceBusterException e) {
			logger.error("Exception updating experience entity relations, expId: "
					+ exp.getId());
			throw e;
		}
		storage.updateObject(exp);
	}

	public void removeContent(String expId, String contentId)
			throws NotFoundException, DataException, ExperienceBusterException {
		Content temp = new Content();
		temp.setId(contentId);
		removeContent(expId, temp);
	}

	public List<Experience> search(User user, Integer position, Integer size,
			Integer count, Long since, ExperienceFilter filter) {
		return storage.search(user, position, size, count, since, filter);
	}

	private Content getContentById(String expId, String contentId)
			throws NotFoundException, DataException {

		Experience exp = getById(expId);
		for (Content t : exp.getContents()) {
			if (t.getId().equals(contentId)) {
				return t;
			}
		}
		throw new NotFoundException("");
	}

	private Long createSocialEntity(Experience exp)
			throws ExperienceBusterException {
		try {
			Entity entity = SemanticHelper.createEntity(socialClient, exp
					.getSocialUserId(), "experience", exp.getTitle(), exp
					.getDescription(), exp.getTags() != null ? exp.getTags()
					: null, null);
			exp.setEntityId(entity.getId());
		} catch (WebApiException e) {
			throw new ExperienceBusterException();
		}
		return exp.getEntityId();
	}

	private List<Long> createSocialEntity(Content[] contents, long socialUserId)
			throws ExperienceBusterException {
		List<Long> ids = new ArrayList<Long>();
		for (Content c : contents) {
			Long id = null;
			if ((id = createSocialEntity(c, socialUserId)) != null)
				ids.add(id);
		}
		return ids;
	}

	private void deleteSocialEntity(Content content)
			throws ExperienceBusterException {
		if (isSocialContent(content)) {
			long eid = content.getEntityId();
			try {
				SemanticHelper.deleteEntity(socialClient, eid);
			} catch (WebApiException e) {
				logger.error(String
						.format("Exception deleting social entity, entityId: %s, contentId: %s",
								eid, content.getId()));
				throw new ExperienceBusterException();
			}
		}
	}

	private void deleteSocialEntity(Experience exp)
			throws ExperienceBusterException {
		try {
			SemanticHelper.deleteEntity(socialClient, exp.getEntityId());
		} catch (WebApiException e) {
			logger.error(String
					.format("Exception deleting social entity, entityId: %s, experienceId: %s",
							exp.getEntityId(), exp.getId()));
			throw new ExperienceBusterException();
		}
	}

	private Long createSocialEntity(Content content, long socialUserId)
			throws ExperienceBusterException {
		if (isSocialContent(content)) {

			try {
				Entity entity = SemanticHelper.createEntity(socialClient,
						socialUserId, "computer file", content.getType()
								.toString(), null, null, null);
				content.setEntityId(entity.getId());
				content.setEntityType("computer file");
				return entity.getId();
			} catch (WebApiException e) {
				throw new ExperienceBusterException();
			}
		}
		return null;
	}

	private boolean checkCollectionConsistency(Experience exp)
			throws NumberFormatException, DataException {
		List<String> ids = new ArrayList<String>();
		try {
			UserPreference pref = prefManager.get(exp.getUser()).get(0);
			for (ExpCollection coll : pref.getCollections()) {
				ids.add(coll.getId());
			}
		} catch (NotFoundException e) {
		}
		return exp.getCollectionIds() == null
				|| ids.containsAll(exp.getCollectionIds());

	}

	private boolean checkContent(Experience exp, String contentId) {
		for (Content c : exp.getContents()) {
			if (c.getId().equals(contentId)) {
				return true;
			}
		}
		return false;
	}

	private boolean isUploadableContent(Content content) {
		return content.getType() == ContentType.PHOTO
				|| content.getType() == ContentType.FILE
				|| content.getType() == ContentType.AUDIO;
	}

	private boolean isSocialContent(Content content) {
		return content.getType() == ContentType.VIDEO
				|| content.getType() == ContentType.PHOTO
				|| content.getType() == ContentType.FILE
				|| content.getType() == ContentType.AUDIO;
	}

}
