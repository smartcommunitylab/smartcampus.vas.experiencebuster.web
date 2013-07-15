package eu.trentorise.smartcampus.vas.experiencebuster.manager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.eb.model.ModeratedContent;

@Component
public class ModeratedContentManager {

	@Autowired
	@Qualifier("moderatedDao")
	MongoTemplate moderatedDb;

	private static ObjectId lastElement;

	public List<ModeratedContent> getModeratedContents(boolean isApproved,
			boolean incremental) {
		Criteria criteria = new Criteria();
		if (incremental && lastElement != null) {
			criteria.and("id").gt(lastElement);
		}
		criteria.and("approved").is(isApproved);

		Query query = Query.query(criteria);
		List<ModeratedContent> results = moderatedDb.find(query,
				ModeratedContent.class);

		// save last result
		if (!results.isEmpty()) {
			Collections.sort(results, idComparator);
			ObjectId lastId = new ObjectId(results.get(results.size() - 1)
					.getId());
			if (lastElement == null || lastElement.compareTo(lastId) < 0) {
				lastElement = lastId;
			}
		}
		return results;
	}

	private Comparator<ModeratedContent> idComparator = new Comparator<ModeratedContent>() {
		@Override
		public int compare(ModeratedContent m1, ModeratedContent m2) {
			return m1.getId().compareTo(m2.getId());
		}
	};
}
