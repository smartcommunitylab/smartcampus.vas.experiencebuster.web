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
package eu.trentorise.smartcampus.vas.experiencebuster.model;

import java.util.List;

import eu.trentorise.smartcampus.presentation.data.BasicObject;

public class UserPreference extends BasicObject {
	private static final long serialVersionUID = -4771192399208749425L;
	private long socialUserId;
	private List<ExpCollection> collections;

	public long getSocialUserId() {
		return socialUserId;
	}

	public void setSocialUserId(long socialUserId) {
		this.socialUserId = socialUserId;
	}

	public List<ExpCollection> getCollections() {
		return collections;
	}

	public void setCollections(List<ExpCollection> collections) {
		this.collections = collections;
	}

}
