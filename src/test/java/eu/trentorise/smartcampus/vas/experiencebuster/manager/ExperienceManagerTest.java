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

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import eu.trentorise.smartcampus.ac.provider.model.User;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.vas.experiencebuster.filter.ExperienceFilter;
import eu.trentorise.smartcampus.vas.experiencebuster.model.Content;
import eu.trentorise.smartcampus.vas.experiencebuster.model.ContentType;
import eu.trentorise.smartcampus.vas.experiencebuster.model.ExpCollection;
import eu.trentorise.smartcampus.vas.experiencebuster.model.Experience;
import eu.trentorise.smartcampus.vas.experiencebuster.model.UserPreference;

public class ExperienceManagerTest extends SocialEngineOperation {

	private static ExperienceManager expManager;
	private static PreferenceManager prefManager;
	private static User user;

	public ExperienceManagerTest() {

	}

	@BeforeClass
	public static void init() throws WebApiException, DataException {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"spring/applicationContext.xml");
		expManager = ctx.getBean(ExperienceManager.class);
		prefManager = ctx.getBean(PreferenceManager.class);
	}

	@Before
	public void environment() throws WebApiException, DataException {
		setupTestUser(-1);
		// setupTestUser(438);
		setupPreference(user);
	}

	@After
	public void cleanup() throws WebApiException, NotFoundException,
			DataException, ExperienceBusterException {
		for (Experience e : expManager.getAll(user)) {
			expManager.removeExperience(e.getId());
		}

		for (UserPreference u : prefManager.get(user)) {
			prefManager.remove(u);
		}

	}

	@Test(expected = DataException.class)
	public void createWithoutContent() throws DataException, NotFoundException {
		Experience exp = createExperience();
		expManager.store(user, exp, null);
	}

	@Test
	public void addContent() throws DataException, NotFoundException,
			ExperienceBusterException {
		Experience exp = createExperienceWithContent(ContentType.TEXT);
		exp = expManager.store(user, exp, null);
		Assert.assertEquals(1, expManager.getById(exp.getId()).getContents()
				.size());
		expManager.addContent(exp.getId(), createContent(ContentType.TEXT),
				null);
		Assert.assertEquals(2, expManager.getById(exp.getId()).getContents()
				.size());
	}

	@Test
	public void removeContent() throws DataException, NotFoundException,
			ExperienceBusterException {
		Experience exp = createExperienceWithContent(ContentType.TEXT);
		exp = expManager.store(user, exp, null);

		expManager.addContent(exp.getId(),
				createContent("id_content", ContentType.TEXT), null);
		Assert.assertEquals(2, expManager.getById(exp.getId()).getContents()
				.size());
		expManager.removeContent(exp.getId(), "id_content");
		Assert.assertEquals(1, expManager.getById(exp.getId()).getContents()
				.size());
	}

	@Test(expected = DataException.class)
	public void removeContentFailure() throws DataException, NotFoundException,
			ExperienceBusterException {
		Experience exp = createExperience();
		exp.setContents(Arrays.asList(createContent("id_content",
				ContentType.TEXT)));
		exp = expManager.store(user, exp, null);
		expManager.removeContent(exp.getId(), "id_content");

	}

	@Test
	public void create() throws DataException, NotFoundException {
		Experience exp = createExperienceWithContent(ContentType.TEXT);

		Assert.assertEquals(0, expManager.getAll(user).size());
		expManager.store(user, exp, null);
		Assert.assertEquals(1, expManager.getAll(user).size());
	}

	@Test
	public void createVideoExp() throws DataException, NotFoundException {
		Experience exp = createExperienceWithContent(ContentType.VIDEO);
		Assert.assertEquals(0, expManager.getAll(user).size());
		expManager.store(user, exp, null);
		Assert.assertEquals(1, expManager.getAll(user).size());
	}

	@Test(expected = DataException.class)
	public void notConsistentCollectionId() throws DataException {
		Experience exp = createExperienceWithContent(ContentType.TEXT);
		exp.setCollectionIds(Arrays.asList("idtest"));
		expManager.store(user, exp, null);
	}

	@Test
	public void collectionIds() throws DataException {

		Experience exp = createExperienceWithContent(ContentType.TEXT);
		exp.setCollectionIds(Arrays.asList("id1"));
		expManager.store(user, exp, null);
	}

	@Test
	public void search() throws DataException {
		Experience exp = createExperienceWithContent(ContentType.TEXT);
		expManager.store(user, exp, null);
		ExperienceFilter filter = new ExperienceFilter();
		filter.setText("title");
		List<Experience> results = expManager.search(user, null, null, null,
				null, filter);
		Assert.assertEquals(1, results.size());

		filter.setCollectionIds(new String[] { "id1" });
		results = expManager.search(user, null, null, null, null, filter);
		Assert.assertEquals(1, results.size());

		filter.setCollectionIds(new String[] { "id2" });
		results = expManager.search(user, null, null, null, null, filter);
		Assert.assertEquals(0, results.size());

	}

	private static void setupPreference(User user) throws DataException {
		UserPreference pref = new UserPreference();
		pref.setCollections(Arrays.asList(new ExpCollection("id1", "coll1")));
		prefManager.create(user, pref);
	}

	private static void setupTestUser(long socialUserId) throws WebApiException {
		user = new User();
		user.setId(1L);
		if (socialUserId <= 0) {
			socialUserId = createUser().getId();
		}
		user.setSocialId(socialUserId);
		user.setAuthToken("105e8e80-4822-4612-ae24-a8b022f1bc94");
		user.setExpTime(1L);
	}

	private Experience createExperience() {
		Experience exp = new Experience();
		exp.setAddress("address");
		exp.setTitle("title");
		exp.setDescription("description");
		return exp;
	}

	private Experience createExperienceWithContent(ContentType contentType) {
		Experience exp = createExperience();
		exp.setCollectionIds(Arrays.asList("id1"));
		exp.setContents(Arrays.asList(createContent(contentType)));
		return exp;
	}

	private Content createContent(ContentType contentType) {
		return createContent(null, contentType);
	}

	private Content createContent(String id, ContentType contentType) {
		Content c = new Content();
		if (id != null) {
			c.setId(id);
		}
		c.setType(contentType);
		c.setNote("Text of the content");
		return c;
	}

}
