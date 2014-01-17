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
package eu.trentorise.smartcampus.vas.experiencebuster.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.eb.model.Experience;
import eu.trentorise.smartcampus.eb.model.UserPreference;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.presentation.data.BasicObject;
import eu.trentorise.smartcampus.presentation.data.SyncData;
import eu.trentorise.smartcampus.presentation.data.SyncDataRequest;
import eu.trentorise.smartcampus.presentation.storage.sync.BasicObjectSyncStorage;
import eu.trentorise.smartcampus.profileservice.model.BasicProfile;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.ExperienceBusterException;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.ExperienceManager;

/**
 * @author mirko perillo
 * 
 */
@Controller
public class SyncController extends RestController {

	private static final Logger logger = Logger.getLogger(SyncController.class);

	@Autowired
	private BasicObjectSyncStorage storage;

	@Autowired
	private ExperienceManager expManager;

	@RequestMapping(method = RequestMethod.POST, value = "/sync")
	public @ResponseBody
	SyncData synchronize(HttpServletRequest request,
			HttpServletResponse response, @RequestParam long since,
			@RequestBody Map<String, Object> obj) throws Exception {

		BasicProfile user = getUserProfile();

		SyncDataRequest syncReq = Util.convertRequest(obj, since);

		List<String> prefsToDelete = new ArrayList<String>();
		try {
			associateUserData(syncReq.getSyncData(), user, prefsToDelete);
		} catch (ExperienceBusterException e) {
			e.printStackTrace();
			logger.error("Exception associating social info to experiences");
			throw e;
		}

		SyncData result = storage.getSyncData(syncReq.getSince(),
				user.getUserId());

		// added updating of experiences
		result.getUpdated().putAll(syncReq.getSyncData().getUpdated());

		// delete from client old userPreference
		if (!prefsToDelete.isEmpty()
				&& result.getDeleted().get(
						UserPreference.class.getCanonicalName()) == null) {
			result.getDeleted().put(UserPreference.class.getCanonicalName(),
					new ArrayList<String>());
			result.getDeleted().get(UserPreference.class.getCanonicalName())
					.addAll(prefsToDelete);
		}

		storage.cleanSyncData(syncReq.getSyncData(), "" + user.getUserId());
		return result;
	}

	private void associateUserData(SyncData dataClient, BasicProfile user,
			List<String> preferencesToDelete) throws ExperienceBusterException {
		if (dataClient != null && dataClient.getUpdated() != null) {
			List<BasicObject> obj = dataClient.getUpdated().get(
					Experience.class.getCanonicalName());
			if (obj != null) {
				for (BasicObject o : obj) {
					Experience exp = (Experience) o;
					// check if experience have relative entityId
					expManager.associateSocialData(exp, user);
				}
			}
			obj = dataClient.getUpdated().get(
					UserPreference.class.getCanonicalName());
			if (obj != null) {
				UserPreference pref = null;
				UserPreference toRemove = null;
				for (BasicObject o : obj) {
					UserPreference newPref = (UserPreference) o;

					if (newPref.getSocialUserId() <= 0) {
						try {
							List<UserPreference> prefs = storage
									.getObjectsByType(UserPreference.class,
											user.getUserId());
							if (!prefs.isEmpty()) {
								if (prefs.size() > 1) {
									logger.warn(String
											.format("User %s has multiple UserPreference associated",
													o.getUser()));
								}
								pref = prefs.get(0);
								if (newPref.getCollections() != null
										&& !newPref.getCollections().isEmpty()) {
									pref.getCollections().addAll(
											newPref.getCollections());
									storage.storeObject(pref);
								}
								toRemove = newPref;
								preferencesToDelete.add(o.getId());
							}

						} catch (DataException e) {
							logger.error("Exception checking preferences of user "
									+ user.getUserId());
							e.printStackTrace();
						}
					}
					// update user ID
					o.setUser(user.getUserId());
					try {
						((UserPreference) o).setSocialUserId(new Long(user
								.getSocialId()));
					} catch (Exception e) {
						logger.error(String
								.format("Exception associated user data to UserPreference user: %s, socialId: %s",
										user.getUserId(), user.getSocialId()));
					}
				}
				if (pref != null) {
					dataClient.getUpdated()
							.get(UserPreference.class.getCanonicalName())
							.add(pref);
				}
				if (toRemove != null) {
					dataClient.getUpdated()
							.get(UserPreference.class.getCanonicalName())
							.remove(toRemove);
				}
			}
		}
		if (dataClient != null && dataClient.getDeleted() != null) {
			List<String> obj = dataClient.getDeleted().get(
					Experience.class.getCanonicalName());
			if (obj != null) {
				for (String o : obj) {
					try {
						// check if experience have relative entityId
						expManager.removeSocialData(o, user);
					} catch (Exception e) {
						e.printStackTrace();
						// throw new ExperienceBusterException(e.getMessage());
					}
				}
			}
		}
	}
}
