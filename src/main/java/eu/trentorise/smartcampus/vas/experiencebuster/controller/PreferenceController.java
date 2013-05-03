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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.ac.provider.model.User;
import eu.trentorise.smartcampus.eb.model.UserPreference;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.Permission;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.PreferenceManager;

@Controller("prefController")
public class PreferenceController extends RestController {

	@Autowired
	PreferenceManager prefManager;

	@RequestMapping(method = RequestMethod.GET, value = "/eu.trentorise.smartcampus.eb.model.UserPreference")
	public @ResponseBody
	List<UserPreference> get(HttpServletRequest request,
			HttpServletResponse response) throws DataException,
			NotFoundException {
		User user = retrieveUser(request, response);

		return prefManager.get(user);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/eu.trentorise.smartcampus.eb.model.UserPreference/{pid}")
	public @ResponseBody
	void update(HttpServletRequest request, HttpServletResponse response,
			@RequestBody UserPreference pref, @PathVariable("pid") String pid)
			throws DataException, NotFoundException {
		User user = retrieveUser(request, response);
		if (!prefManager.checkPermission(user, pref, Permission.UPDATE)) {
			throw new SecurityException();
		}
		pref.setId(pid);
		prefManager.update(pref);
	}
}
