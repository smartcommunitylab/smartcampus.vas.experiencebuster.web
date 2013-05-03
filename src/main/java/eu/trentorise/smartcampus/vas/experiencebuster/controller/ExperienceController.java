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

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import eu.trentorise.smartcampus.ac.provider.model.User;
import eu.trentorise.smartcampus.eb.model.Content;
import eu.trentorise.smartcampus.eb.model.Experience;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.vas.experiencebuster.filter.ExperienceFilter;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.ExperienceBusterException;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.ExperienceManager;
import eu.trentorise.smartcampus.vas.experiencebuster.manager.Permission;

@Controller("experienceController")
public class ExperienceController extends RestController {

	@Autowired
	ExperienceManager expManager;

	@RequestMapping(method = RequestMethod.GET, value = "/eu.trentorise.smartcampus.eb.model.Experience")
	public @ResponseBody
	List<Experience> get(HttpServletRequest request,
			HttpServletResponse response, HttpSession session)
			throws DataException, NotFoundException {

		User user = retrieveUser(request, response);
		return expManager.getAll(user);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/eu.trentorise.smartcampus.eb.model.Experience/{eid}")
	public @ResponseBody
	Experience get(HttpServletRequest request, HttpServletResponse response,
			HttpSession session, @PathVariable("eid") String eid)
			throws DataException, NotFoundException, ExperienceBusterException {

		User user = retrieveUser(request, response);

		if (!expManager.checkPermission(eid, user, Permission.READ)) {
			throw new SecurityException();
		}

		return expManager.getById(eid);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/eu.trentorise.smartcampus.eb.model.Experience")
	public @ResponseBody
	void createExp(HttpServletRequest request, HttpServletResponse response,
			HttpSession session,
			@RequestParam("exp") String jsonRepresentation,
			@RequestParam(required = false, value = "file") MultipartFile file)
			throws DataException, IOException {

		User user = retrieveUser(request, response);

		ObjectMapper mapper = new ObjectMapper();
		try {
			Experience exp = mapper.readValue(
					jsonRepresentation.replace("'", "\""), Experience.class);
			expManager
					.store(user, exp, (file != null) ? file.getBytes() : null);
		} catch (JsonParseException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/eu.trentorise.smartcampus.eb.model.Experience/{eid}")
	public @ResponseBody
	void updateExp(HttpServletRequest request, HttpServletResponse response,
			HttpSession session,
			@RequestParam("exp") String jsonRepresentation,
			@PathVariable("eid") String eid) throws DataException, IOException,
			NotFoundException, ExperienceBusterException {
		User user = retrieveUser(request, response);

		if (!expManager.checkPermission(eid, user, Permission.UPDATE)) {
			throw new SecurityException();
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			Experience exp = mapper.readValue(
					jsonRepresentation.replace("'", "\""), Experience.class);
			exp.setId(eid);
			expManager.update(exp);
		} catch (JsonParseException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}

	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/eu.trentorise.smartcampus.eb.model.Experience/{eid}")
	public @ResponseBody
	void deleteExp(HttpServletRequest request, HttpServletResponse response,
			HttpSession session, @PathVariable("eid") String eid)
			throws DataException, NotFoundException, ExperienceBusterException {

		User user = retrieveUser(request, response);

		if (!expManager.checkPermission(eid, user, Permission.DELETE)) {
			throw new SecurityException();
		}

		expManager.removeExperience(eid);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/eu.trentorise.smartcampus.eb.model.Content/{eid}")
	public @ResponseBody
	void addContent(HttpServletRequest request, HttpServletResponse response,
			HttpSession session, @PathVariable("eid") String eid,
			@RequestParam("content") String jsonRepresentation,
			@RequestParam("file") MultipartFile file) throws DataException,
			NotFoundException, ExperienceBusterException, IOException {

		User user = retrieveUser(request, response);

		if (!expManager.checkPermission(eid, user, Permission.UPDATE)) {
			throw new SecurityException();
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			Content content = mapper.readValue(
					jsonRepresentation.replace("'", "\""), Content.class);
			expManager.addContent(eid, content,
					file == null ? null : file.getBytes());
		} catch (JsonParseException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/eu.trentorise.smartcampus.eb.model.Content/{eid}/{cid}")
	public @ResponseBody
	void removeContent(HttpServletRequest request,
			HttpServletResponse response, HttpSession session,
			@PathVariable("eid") String eid, @PathVariable("cid") String cid)
			throws DataException, NotFoundException, ExperienceBusterException {
		User user = retrieveUser(request, response);

		if (!expManager.checkPermission(eid, user, Permission.UPDATE)) {
			throw new SecurityException();
		}

		expManager.removeContent(eid, cid);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/eu.trentorise.smartcampus.eb.model.Content/{eid}/{cid}")
	public @ResponseBody
	void updateContent(HttpServletRequest request,
			HttpServletResponse response, HttpSession session,
			@PathVariable("eid") String eid,
			@RequestParam("content") String jsonRepresentation)
			throws DataException, NotFoundException, ExperienceBusterException,
			IOException {
		User user = retrieveUser(request, response);

		if (!expManager.checkPermission(eid, user, Permission.UPDATE)) {
			throw new SecurityException();
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			Content content = mapper.readValue(
					jsonRepresentation.replace("'", "\""), Content.class);
			expManager.updateContent(eid, content);
		} catch (JsonParseException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/objects")
	public @ResponseBody
	List<Experience> search(HttpServletRequest request,
			HttpServletResponse response, HttpSession session,
			@RequestParam("filter") String jsonFilter,
			@RequestParam Integer position, @RequestParam Integer size,
			@RequestParam Integer count, @RequestParam Long since)
			throws DataException, NotFoundException, ExperienceBusterException,
			IOException {
		ExperienceFilter filter = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			filter = mapper.readValue(jsonFilter, ExperienceFilter.class);
		} catch (JsonMappingException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}

		User user = retrieveUser(request, response);

		return expManager.search(user, position, size, count, since, filter);
	}

}
