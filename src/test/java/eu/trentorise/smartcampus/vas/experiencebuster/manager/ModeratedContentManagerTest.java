package eu.trentorise.smartcampus.vas.experiencebuster.manager;

import junit.framework.Assert;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.trentorise.smartcampus.eb.model.ModeratedContent;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(value = "/spring/applicationContext.xml")
public class ModeratedContentManagerTest {

	@Autowired
	@Qualifier(value = "moderatedDao")
	private MongoTemplate dao;

	@Autowired
	private ModeratedContentManager manager;

	@After
	public void clean() {
		dao.remove(new Query(), ModeratedContent.class);
	}

	@Before
	public void setup() {
		if (dao.findAll(ModeratedContent.class).size() == 0) {
			ModeratedContent c = new ModeratedContent();
			c.setId(new ObjectId().toString());
			c.setApproved(true);
			c.setContentType("image/jpg");
			c.setName("M_434343");

			dao.save(c);

			c = new ModeratedContent();
			c.setId(new ObjectId().toString());
			c.setApproved(false);
			c.setContentType("image/jpg");
			c.setName("M_454545");

			dao.save(c);

			c = new ModeratedContent();
			c.setId(new ObjectId().toString());
			c.setApproved(true);
			c.setContentType("image/jpg");
			c.setName("M_474747");

			dao.save(c);

			c = new ModeratedContent();
			c.setId(new ObjectId().toString());
			c.setApproved(true);
			c.setContentType("image/jpg");
			c.setName("M_404040");

			dao.save(c);
		}
	}

	@Test
	public void query() {
		// Assert.assertEquals(1, manager.getModeratedContents(false,
		// true).size());
		Assert.assertEquals(3, manager.getModeratedContents(true, true).size());
		ModeratedContent c = new ModeratedContent();
		c.setId(new ObjectId().toString());
		c.setApproved(true);
		c.setContentType("image/jpg");
		c.setName("M_303030");
		dao.save(c);
		// Assert.assertEquals(4, manager.getModeratedContents(true,
		// false).size());
		Assert.assertEquals(1, manager.getModeratedContents(true, true).size());
		c = new ModeratedContent();
		c.setId(new ObjectId().toString());
		c.setApproved(false);
		c.setContentType("image/jpg");
		c.setName("M_3030303");
		dao.save(c);

		c = new ModeratedContent();
		c.setId(new ObjectId().toString());
		c.setApproved(false);
		c.setContentType("image/jpg");
		c.setName("M_3030301");
		dao.save(c);
		// Assert.assertEquals(2, manager.getModeratedContents(false, false)
		// .size());
		// Assert.assertEquals(2, manager.getModeratedContents(false,
		// true).size());
		Assert.assertEquals(0, manager.getModeratedContents(true, true).size());
	}
}
