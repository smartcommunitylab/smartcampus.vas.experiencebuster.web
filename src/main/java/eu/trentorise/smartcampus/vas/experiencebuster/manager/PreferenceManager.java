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

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.ac.provider.model.User;
import eu.trentorise.smartcampus.eb.model.UserPreference;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.vas.experiencebuster.storage.ExperienceStorage;

@Component
public class PreferenceManager {

	@Autowired
	ExperienceStorage storage;

	private static final Logger logger = Logger
			.getLogger(PreferenceManager.class);

	public List<UserPreference> get(User user) throws DataException,
			NotFoundException {
		List<UserPreference> result = storage.getObjectsByType(
				UserPreference.class, Utils.userId(user));
		if (result.size() == 0) {
			UserPreference pref = new UserPreference();
			create(user, pref);
		}
		return result;
	}

	public List<UserPreference> get(String user) throws DataException,
			NotFoundException {
		List<UserPreference> result = storage.getObjectsByType(
				UserPreference.class, user);
		if (result.size() == 0) {
			UserPreference pref = new UserPreference();
			create(user, pref);
		}
		return result;
	}

	public void remove(UserPreference pref) throws DataException {
		storage.deleteObject(pref);
	}

	public void create(User user, UserPreference pref) throws DataException {
		String userId = Utils.userId(user);
		createPreference(userId, pref);
	}

	public void create(String user, UserPreference pref) throws DataException {
		createPreference(user, pref);
	}

	private void createPreference(String userId, UserPreference pref)
			throws DataException {
		if (storage.getObjectsByType(UserPreference.class, userId).size() > 1) {
			logger.warn("More than one preference object for user: "
					+ pref.getUser());
			throw new DataException(
					"More than one preference object for user: "
							+ pref.getUser());
		}
		storage.processStoringCollections(pref.getCollections());
		pref.setUser(userId);
		storage.storeObject(pref);
	}

	public void update(UserPreference pref) throws NotFoundException,
			DataException {
		storage.processStoringCollections(pref.getCollections());
		storage.updateObject(pref);
	}

	public boolean checkPermission(User user, UserPreference pref,
			Permission permission) {
		boolean result = false;
		switch (permission) {
		case UPDATE:
			result = pref.getUser().equals(Utils.userId(user));
			break;
		default:
			throw new UnsupportedOperationException();
		}
		return result;
	}

}
