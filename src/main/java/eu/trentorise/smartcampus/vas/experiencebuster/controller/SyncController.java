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

import eu.trentorise.smartcampus.ac.provider.model.User;
import eu.trentorise.smartcampus.controllers.SCController;
import eu.trentorise.smartcampus.eb.model.Experience;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.presentation.data.BasicObject;
import eu.trentorise.smartcampus.presentation.data.SyncData;
import eu.trentorise.smartcampus.presentation.data.SyncDataRequest;
import eu.trentorise.smartcampus.presentation.storage.sync.BasicObjectSyncStorage;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.ExperienceBusterException;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.ExperienceManager;

/**
 * @author mirko perillo
 * 
 */
@Controller
public class SyncController extends SCController {

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

		User user = retrieveUser(request);
		if (user == null) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		SyncDataRequest syncReq = Util.convertRequest(obj, since);
		SyncData result = storage.getSyncData(syncReq.getSince(),
				"" + user.getId());
		
		// check if experience have relative entityId
		try{
		associateSocialData(syncReq.getSyncData(), user.getSocialId(), Experience.class);
		}catch(ExperienceBusterException e){
			logger.error("Exception associating social info to experiences");
			throw e;
		}
		
		//added updating of experiences
		result.getUpdated().putAll(syncReq.getSyncData().getUpdated());
		
		storage.cleanSyncData(syncReq.getSyncData(), "" + user.getId());
		return result;
	}

	
	private void associateSocialData(SyncData data,long socialUserId, Class classname) throws ExperienceBusterException{
		if(data != null && data.getUpdated() != null){
		List<BasicObject> obj =	data.getUpdated().get(classname.getCanonicalName());
		if(obj != null){
			for(BasicObject o : obj){
				if(o instanceof Experience){
					Experience exp = (Experience) o;
				expManager.associateSocialData(exp, socialUserId);
				}
			}
		}
		}
	}
	
}