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
import java.util.Iterator;
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
import eu.trentorise.smartcampus.filestorage.client.Filestorage;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.data.BasicObject;
import eu.trentorise.smartcampus.profileservice.model.BasicProfile;
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

	@Autowired
	Filestorage filestorage;

	@Autowired
	private SecurityManager securityManager;

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

	public List<Experience> getAll(BasicProfile user) throws NotFoundException,
			DataException {
		return storage.getObjectsByType(Experience.class, user.getUserId());
	}

	public Experience getById(String id) throws NotFoundException,
			DataException {
		return storage.getObjectById(id, Experience.class);
	}

	public Content storeContent(Experience exp, Content content, byte[] file)
			throws ExperienceBusterException {

		storage.processStoringContent(content);

		if (isUploadableContent(content) && file != null) {
			Long fid = fileManager.updload(Long.valueOf(exp.getSocialUserId()),
					file);
			content.setValue(fid.toString());
		}

		createSocialEntity(content, exp.getSocialUserId());
		return content;

	}

	public Experience store(BasicProfile user, Experience exp, byte[] file)
			throws DataException {
		if (exp.getContents() == null || exp.getContents().size() != 1) {
			throw new DataException(
					"Experience must contain only a content during creation");
		}
		exp.setUser(user.getUserId());
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
		if (experience.getUser() == null) experience.setUser(saved.getUser());
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
			result = object.getSocialUserId().equals(user.getSocialId())
					&& checkContent(object, cid);
			break;
		default:
			throw new UnsupportedOperationException();
		}

		return result;
	}

	public boolean checkPermission(String eid, BasicProfile user,
			Permission permission) throws NotFoundException, DataException,
			UnsupportedOperationException, ExperienceBusterException {
		Experience object = storage.getObjectById(eid, Experience.class);
		if (object == null) return true;
		
		boolean result = false;
		switch (permission) {
		case UPDATE:
		case DELETE:
			result = object.getUser().equals(user.getUserId());
			break;
		case READ:
			result = object.getUser().equals(user.getUserId());
			if (!result) {
				try {
					result = SemanticHelper.isEntitySharedWithUser(
							socialClient, Long.valueOf(object.getEntityId()),
							new Long(user.getSocialId()));
				} catch (WebApiException e) {
					throw new ExperienceBusterException();
				}
			}
			break;
		default:
			throw new UnsupportedOperationException();
		}

		return result;
	}

	public Experience associateSocialData(Experience exp, BasicProfile user)
			throws ExperienceBusterException {
		
		if (exp.getId() != null) {
			Experience original = null;
			try {
				original = storage.getObjectById(exp.getId(),Experience.class);
				if (exp.getSocialUserId() == null) exp.setSocialUserId(original.getSocialUserId());
				if (exp.getEntityId() == null) exp.setEntityId(original.getEntityId());
			} catch (NotFoundException e) {
			} catch (DataException e) {
				e.printStackTrace();
			}
		}
		
		if (exp.getSocialUserId() != null && !exp.getSocialUserId().equals(user.getSocialId())) {
			throw new ExperienceBusterException("Non matching social IDs: "+user.getSocialId()+" and "+exp.getSocialUserId());
		}
		if (exp.getSocialUserId() == null) {
			exp.setSocialUserId(user.getSocialId());
		}

		if (exp.getUser() == null) {
			exp.setUser(user.getUserId());
		}

		if (!isValidEntityId(exp.getEntityId())) {
			createSocialEntity(exp);
			logger.info(String.format("Associated exp %s with entityId %s",
					exp.getId(), exp.getEntityId()));
		} else {
			try {
			SemanticHelper.updateEntity(socialClient,
					Long.valueOf(exp.getEntityId()), exp.getTitle(),
					exp.getDescription(), exp.getTags(), null);
			} catch (WebApiException e) {
				throw new ExperienceBusterException();
			}

		}

		for (Content c : exp.getContents()) {
			if (!isUploadableContent(c)) continue;
			if (!isValidEntityId(c.getEntityId())) {
				// content has the same entityId of experience
				c.setEntityId(exp.getEntityId());
				logger.info(String.format(
						"Associated content %s of exp %s with entityId %s",
						c.getId(), exp.getId(), c.getEntityId()));
				// update resource social informations
				try {
					if (c.getValue() != null
							&& !c.getValue().equals(c.getLocalValue())) {
						filestorage.updateSocialDataByApp(
								securityManager.getSecurityToken(),
								c.getValue(), "" + exp.getEntityId());
						logger.info(String.format(
								"Updated social data of content %s", c.getId()));
					} else {
						logger.warn(String
								.format("Resource id %s, update social data not possible",
										c.getValue()));
					}
				} catch (Exception e) {
					logger.error(String
							.format("Exception updating social data content %s (resourceId: %s)",
									c.getId(), c.getValue()));
				}
			}
		}

		return exp;
	}

	private boolean isValidEntityId(String entityId) {
		try {
			return entityId != null && entityId.length() > 0
					&& Long.valueOf(entityId) > 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void updateEntityRelations(Experience exp)
			throws ExperienceBusterException {
		List<Long> entityIds = new ArrayList<Long>();
		for (Content c : exp.getContents()) {
			if (isValidEntityId(c.getEntityId())) {
				entityIds.add(Long.valueOf(c.getEntityId()));
			}
		}
		try {
			SemanticHelper.updateEntity(socialClient,
					Long.valueOf(exp.getEntityId()), null, null, null,
					entityIds.isEmpty() ? null : entityIds);
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
			SemanticHelper.updateEntity(socialClient,
					Long.valueOf(source.getEntityId()), source.getTitle(),
					source.getDescription(), source.getTags(), null);
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

	public List<Experience> search(BasicProfile user, Integer position,
			Integer count, Long since, ExperienceFilter filter, boolean useCurrent) throws ExperienceBusterException {
		List<Experience> list = storage.search(useCurrent ? user : null, position, count, since, filter);
		for (Iterator<Experience> iterator = list.iterator(); iterator.hasNext();) {
			Experience experience = iterator.next();
			boolean result = experience.getUser().equals(user.getUserId());
			if (!result) {
				try {
					result = SemanticHelper.isEntitySharedWithUser(
							socialClient, Long.valueOf(experience.getEntityId()),
							new Long(user.getSocialId()));
				} catch (WebApiException e) {
					throw new ExperienceBusterException();
				}
			}
			if (!result) throw new SecurityException("Attempt reading non-owned or non-shared entity");
		}
		return list;
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

	private String createSocialEntity(Experience exp)
			throws ExperienceBusterException {
		try {
			Entity entity = SemanticHelper.createEntity(socialClient, Long
					.valueOf(exp.getSocialUserId()), "experience", exp
					.getTitle(), exp.getDescription(),
					exp.getTags() != null ? exp.getTags() : null, null);
			exp.setEntityId(Long.toString(entity.getId()));
		} catch (WebApiException e) {
			throw new ExperienceBusterException();
		}
		return exp.getEntityId();
	}

	private List<Long> createSocialEntity(Content[] contents,
			String socialUserId) throws ExperienceBusterException {
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
		if (content.getEntityId() == null) return;
		if (isSocialContent(content)) {
			long eid = Long.valueOf(content.getEntityId());
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
		if (exp.getEntityId() == null) return;
		try {
			SemanticHelper.deleteEntity(socialClient,
					Long.valueOf(exp.getEntityId()));
		} catch (WebApiException e) {
			logger.error(String
					.format("Exception deleting social entity, entityId: %s, experienceId: %s",
							exp.getEntityId(), exp.getId()));
			throw new ExperienceBusterException();
		}
	}

	private Long createSocialEntity(Content content, String socialUserId)
			throws ExperienceBusterException {
		if (isSocialContent(content)) {

			try {
				Entity entity = SemanticHelper.createEntity(socialClient, Long
						.valueOf(socialUserId), "computer file", content
						.getType().toString(), null, null, null);
				content.setEntityId(Long.toString(entity.getId()));
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

	public void removeSocialData(String eid, BasicProfile user) throws UnsupportedOperationException, NotFoundException, DataException, ExperienceBusterException {
		if (!checkPermission(eid, user, Permission.DELETE)) {
			throw new SecurityException();
		}
		Experience exp = storage.getObjectById(eid, Experience.class);
		if (eid == null) return;
		for (Content content : exp.getContents()) {
			deleteSocialEntity(content);
		}
		deleteSocialEntity(exp);
	}

}
