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
package eu.trentorise.smartcampus.vas.experiencebuster.storage;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import eu.trentorise.smartcampus.eb.model.Content;
import eu.trentorise.smartcampus.eb.model.ExpCollection;
import eu.trentorise.smartcampus.eb.model.Experience;
import eu.trentorise.smartcampus.presentation.storage.sync.mongo.BasicObjectSyncMongoStorage;
import eu.trentorise.smartcampus.profileservice.model.BasicProfile;
import eu.trentorise.smartcampus.vas.experiencebuster.filter.ExperienceFilter;

public class ExperienceStorage extends BasicObjectSyncMongoStorage {

	public ExperienceStorage(MongoOperations mongoTemplate) {
		super(mongoTemplate);
	}

	public String createUniqueId() {
		return new ObjectId().toString();
	}

	public void processStoringCollections(List<ExpCollection> collection) {
		if (collection != null) {
			for (ExpCollection c : collection) {
				processStoringCollections(c);
			}
		}

	}

	public List<Experience> search(BasicProfile user, Integer position,
			Integer size, Integer count, Long since, ExperienceFilter filter) {
		List<Experience> list = find(
				Query.query(createExperienceSearchWithTypeCriteria(
						user.getUserId(), since, filter)), Experience.class);

		Collections.sort(list, arrivalDateComparator);
		if (position != null && count != null && position > 0 && count > 0
				&& list.size() > position) {
			return list.subList(position,
					Math.min(list.size(), position + count));
		}
		return list;

	}

	private Criteria createExperienceSearchWithTypeCriteria(String user,
			Long since, ExperienceFilter filter) {
		Criteria criteria = new Criteria();
		// user is obligatory
		criteria.and("user").is(user);
		// only non-deleted
		criteria.and("deleted").is(false);

		if (since != null) {
			criteria.and("content.timestamp").gte(since);
		}
		if (filter.getCollectionIds() != null) {
			criteria.and("content.collectionIds").is(filter.getCollectionIds());
		}
		if (filter.getConcepts() != null) {
			criteria.and("content.tags").is(filter.getConcepts());
		}
		if (filter.getPlace() != null) {
			// TODO poi ids criteria
		}
		if (filter.getText() != null) {
			criteria.orOperator(
					new Criteria().and("content.title").regex(filter.getText(),
							"i"), new Criteria().and("content.description")
							.regex(filter.getText(), "i"));
		}
		return criteria;
	}

	public void processStoringCollections(ExpCollection object) {
		if (object != null && object.getId() == null) {
			object.setId(createUniqueId());
		}
	}

	public void processStoringContent(List<Content> contents) {
		if (contents != null) {
			for (Content c : contents) {
				processStoringContent(c);
			}
		}

	}

	public void processStoringContent(Content content) {
		if (content != null) {
			if (content.getId() == null) {
				content.setId(createUniqueId());
			}
			content.setTimestamp(System.currentTimeMillis());
		}
	}

	private Comparator<Experience> arrivalDateComparator = new Comparator<Experience>() {
		@Override
		public int compare(Experience o1, Experience o2) {
			return (int) (o1.getUpdateTime() - o2.getUpdateTime());
		}
	};

}
