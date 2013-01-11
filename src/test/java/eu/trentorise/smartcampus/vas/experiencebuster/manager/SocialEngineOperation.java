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
import it.unitn.disi.sweb.webapi.model.entity.Attribute;
import it.unitn.disi.sweb.webapi.model.entity.DataType;
import it.unitn.disi.sweb.webapi.model.entity.Entity;
import it.unitn.disi.sweb.webapi.model.entity.EntityBase;
import it.unitn.disi.sweb.webapi.model.entity.EntityType;
import it.unitn.disi.sweb.webapi.model.entity.Value;
import it.unitn.disi.sweb.webapi.model.smartcampus.social.User;
import it.unitn.disi.sweb.webapi.model.ss.Concept;
import it.unitn.disi.sweb.webapi.model.ss.SemanticString;
import it.unitn.disi.sweb.webapi.model.ss.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

public abstract class SocialEngineOperation {

	private static final Logger logger = Logger
			.getLogger(SocialEngineOperation.class);

	private static final String SE_HOST = "sweb-dev.sc.trentorise.eu";
	private static final int SE_PORT = 9090;

	private static final long testConceptId = 36982L;
	private static final String knownledgeBase = "uk";

	protected static User createUser() throws WebApiException {
		SCWebApiClient client = SCWebApiClient.getInstance(Locale.ENGLISH,
				SE_HOST, SE_PORT);
		EntityBase eb = new EntityBase();
		eb.setLabel("TEST_SC_EB_" + System.currentTimeMillis());
		Long ebId = client.create(eb);
		// Re-read to get the ID of the default KB
		eb = client.readEntityBase(ebId);
		logger.info("Created an entity base " + eb.getLabel() + " with ID "
				+ ebId);
		logger.info("Creating an entity...");
		EntityType person = client.readEntityType("person", eb.getKbLabel());
		Entity entity = new Entity();
		entity.setEntityBase(eb);
		entity.setEtype(person);
		Long eid = client.create(entity);
		logger.info("Created entity with ID " + eid);
		logger.info("Creating a user...");
		User user = new User();
		user.setName("Test user " + System.currentTimeMillis());
		user.setEntityBaseId(eb.getId());
		user.setPersonEntityId(eid);
		long id = client.create(user);
		logger.info("New user's ID: " + id);
		return client.readUser(id);
	}

	protected static void deleteUser(long id) throws WebApiException {
		SCWebApiClient client = SCWebApiClient.getInstance(Locale.ENGLISH,
				SE_HOST, SE_PORT);

		User u = client.readUser(id);
		if (u != null) {
			client.deleteEntity(u.getPersonEntityId());
			client.deleteEntityBase(u.getEntityBaseId());
			if (client.deleteUser(id)) {
				logger.info("Deleted user with ID " + id);
			}
		}
	}

	protected static Entity createEntity(long ebid, EntityTypes type)
			throws WebApiException {

		if (type == null) {
			throw new IllegalArgumentException("type null");
		}
		SCWebApiClient client = SCWebApiClient.getInstance(Locale.ENGLISH,
				SE_HOST, SE_PORT);
		logger.info("Creating an entity base...");
		EntityBase eb1 = client.readEntityBase(ebid);
		EntityType et = client
				.readEntityType(type.toString(), eb1.getKbLabel());

		Entity social = new Entity();
		social.setEntityBase(eb1);
		social.setEtype(et);

		// Entity related = new Entity();
		// related.setEntityBase(eb1);
		// related.setEtype(et);
		// long relatedId = client.create(related);

		List<Attribute> attrs = new ArrayList<Attribute>();
		List<Value> values = new ArrayList<Value>();
		Value v = new Value();
		// String tag attribute
		v.setType(DataType.STRING);
		v.setStringValue("This is a text tag");
		values.add(v);
		Attribute a = new Attribute();
		a.setAttributeDefinition(et.getAttributeDefByName("text"));
		a.setValues(values);
		attrs.add(a);

		// // Entity name
		values = new ArrayList<Value>();
		v = new Value();
		v.setType(DataType.STRING);
		v.setStringValue(type.toString());
		values.add(v);
		a = new Attribute();
		a.setAttributeDefinition(et.getAttributeDefByName("name"));
		a.setValues(values);
		attrs.add(a);

		// Entity description
		// values = new ArrayList<Value>();
		// v = new Value();
		// v.setType(DataType.STRING);
		// v.setStringValue("Event description");
		// values.add(v);
		// a = new Attribute();
		// a.setAttributeDefinition(et.getAttributeDefByName("description"));
		// a.setValues(values);
		// attrs.add(a);

		// Entity tag attribute
		// values = new ArrayList<Value>();
		// v = new Value();
		// v.setType(DataType.RELATION);
		// v.setRelationEntity(client.readEntity(relatedId, null));
		// values.add(v);
		// a = new Attribute();
		// a.setAttributeDefinition(et.getAttributeDefByName("entity"));
		// a.setValues(values);
		// attrs.add(a);

		// Semantic tag attribute
		values = new ArrayList<Value>();
		v = new Value();
		v.setType(DataType.SEMANTIC_STRING);
		// // The semantic string itself
		SemanticString ss = new SemanticString();
		ss.setString("java");
		List<Token> tokens = new ArrayList<Token>();
		Concept c = client.readConceptByGlobalId(36982L, eb1.getKbLabel());
		List<Concept> concepts = new ArrayList<Concept>();
		concepts.add(c);
		Token t = new Token("java", c.getLabel(), c.getId(), concepts);
		tokens.add(t);
		ss.setTokens(tokens);
		v.setSemanticStringValue(ss);
		values.add(v);
		a = new Attribute();
		a.setAttributeDefinition(et.getAttributeDefByName("semantic"));
		a.setValues(values);
		attrs.add(a);

		social.setAttributes(attrs);
		long eid = client.create(social);
		logger.info("Created entity ID:" + eid);
		social = client.readEntity(eid, null);
		return social;
	}

	protected EntityBase createEntityBase() throws WebApiException {
		SCWebApiClient client = SCWebApiClient.getInstance(Locale.ENGLISH,
				SE_HOST, SE_PORT);
		EntityBase eb = new EntityBase();
		eb.setLabel("SC_TEST_EB_" + System.currentTimeMillis());
		long ebid = client.create(eb);
		return client.readEntityBase(ebid);
	}

	protected static void deleteEntity(Entity e) throws WebApiException {
		SCWebApiClient client = SCWebApiClient.getInstance(Locale.ENGLISH,
				SE_HOST, SE_PORT);
		EntityBase eb = e.getEntityBase();
		client.deleteEntity(e.getId());
		client.deleteEntityBase(eb.getId());
	}

	protected static Entity getEntity(long id) throws WebApiException {
		SCWebApiClient client = SCWebApiClient.getInstance(Locale.ENGLISH,
				SE_HOST, SE_PORT);
		return client.readEntity(id, null);
	}

	protected static List<Entity> getEntities(List<Long> ids)
			throws WebApiException {
		SCWebApiClient client = SCWebApiClient.getInstance(Locale.ENGLISH,
				SE_HOST, SE_PORT);
		return client.readEntities(ids, null);
	}

	protected static long getDefaultCommunity() throws WebApiException {
		SCWebApiClient client = SCWebApiClient.getInstance(Locale.ENGLISH,
				SE_HOST, SE_PORT);
		return client.readCommunity("Smartcampus").getId();
	}

	protected static eu.trentorise.smartcampus.common.Concept getTestConcept()
			throws WebApiException {
		SCWebApiClient client = SCWebApiClient.getInstance(Locale.ENGLISH,
				SE_HOST, SE_PORT);
		Concept c = client.readConceptByGlobalId(testConceptId, knownledgeBase);
		eu.trentorise.smartcampus.common.Concept localConcept = new eu.trentorise.smartcampus.common.Concept();
		localConcept.setId(c.getId());
		localConcept.setDescription(c.getDescription());
		localConcept.setName(c.getLabel());
		localConcept.setSummary(c.getSummary());
		return localConcept;

	}

}

enum EntityTypes {
	event, experience, computerFile, journey, location, portfolio, narrative;

	public String toString() {
		switch (this) {
		case event:
			return "event";
		case experience:
			return "experience";
		case computerFile:
			return "computer file";
		case journey:
			return "journey";
		case location:
			return "location";
		case portfolio:
			return "portfolio";
		case narrative:
			return "narrative";
		default:
			return "";
		}
	}

};
