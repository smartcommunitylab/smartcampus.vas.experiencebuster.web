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
package eu.trentorise.smartcampus.eb.model;

import java.util.Arrays;
import java.util.List;

import eu.trentorise.smartcampus.presentation.data.BasicObject;

public class Experience extends BasicObject {
	private static final long serialVersionUID = -6529043503348542953L;
	private String title;
	private String description;
	private String socialUserId;
	private String entityId;
	// private List<Concept> tags;
	private List<String> collectionIds;

	private List<Content> contents;
	private long creationTime;
	private double[] location;
	private String address;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSocialUserId() {
		return socialUserId;
	}

	public void setSocialUserId(String socialUserId) {
		this.socialUserId = socialUserId;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	// public List<Concept> getTags() {
	// return tags;
	// }
	//
	// public void setTags(List<Concept> tags) {
	// this.tags = tags;
	// }

	public List<String> getCollectionIds() {
		return collectionIds;
	}

	public void setCollectionIds(List<String> collectionIds) {
		this.collectionIds = collectionIds;
	}

	public List<Content> getContents() {
		return contents;
	}

	public void setContents(List<Content> contents) {
		this.contents = contents;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public double[] getLocation() {
		return location;
	}

	public void setLocation(double[] location) {
		this.location = location;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "Experience [title=" + title + ", description=" + description
				+ ", socialUserId=" + socialUserId + ", entityId=" + entityId
				/* + ", tags=" + tags */+ ", collectionIds=" + collectionIds
				+ ", contents=" + contents + ", creationTime=" + creationTime
				+ ", location=" + Arrays.toString(location) + ", address="
				+ address + "]";
	}
}
