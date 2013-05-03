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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.ac.provider.model.User;
import eu.trentorise.smartcampus.controllers.SCController;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.presentation.data.SyncData;
import eu.trentorise.smartcampus.presentation.data.SyncDataRequest;
import eu.trentorise.smartcampus.presentation.storage.sync.BasicObjectSyncStorage;

/**
 * @author mirko perillo
 * 
 */
@Controller
public class SyncController extends SCController {

	@Autowired
	private BasicObjectSyncStorage storage;

	@RequestMapping(method = RequestMethod.POST, value = "/sync")
	public @ResponseBody
	SyncData synchronize(HttpServletRequest request,
			HttpServletResponse response, @RequestParam long since,
			@RequestBody Map<String, Object> obj) throws Exception {

		User user = retrieveUser(request);
		if (user == null) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		SyncDataRequest syncReq = Util.convertRequest(obj, since);
		SyncData result = storage.getSyncData(syncReq.getSince(),
				"" + user.getId());
		// filterResult(result, "" + user.getId());
		storage.cleanSyncData(syncReq.getSyncData(), "" + user.getId());

		return result;
	}

	// private void filterResult(SyncData result, String userId) {
	// if (result.getUpdated() != null) {
	// List<BasicObject> list = result.getUpdated().get(
	// EventObject.class.getName());
	// if (list != null && !list.isEmpty()) {
	// for (Iterator<BasicObject> iterator = list.iterator(); iterator
	// .hasNext();) {
	// EventObject event = (EventObject) iterator.next();
	// // skip old events where user does not participate
	// if (event.getFromTime() < System.currentTimeMillis() - 24
	// * 60 * 60 * 1000
	// && (event.getAttending() == null || !event
	// .getAttending().contains(userId))) {
	// iterator.remove();
	// continue;
	// }
	// EventObject.filterUserData((EventObject) event, userId);
	// }
	// }
	// list = result.getUpdated().get(StoryObject.class.getName());
	// if (list != null && !list.isEmpty()) {
	// for (BasicObject story : list) {
	// StoryObject.filterUserData((StoryObject) story, userId);
	// }
	// }
	// }
	// }
}
