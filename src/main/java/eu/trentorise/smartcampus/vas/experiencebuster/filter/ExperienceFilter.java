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
package eu.trentorise.smartcampus.vas.experiencebuster.filter;

import java.io.Serializable;

import eu.trentorise.smartcampus.common.Concept;

public class ExperienceFilter implements Serializable {
	private static final long serialVersionUID = -1960068211542404044L;

	private Concept[] concepts;
	private String[] collectionIds;
	private String place;
	private String text;

	public Concept[] getConcepts() {
		return concepts;
	}

	public void setConcepts(Concept[] concepts) {
		this.concepts = concepts;
	}

	public String[] getCollectionIds() {
		return collectionIds;
	}

	public void setCollectionIds(String[] collectionIds) {
		this.collectionIds = collectionIds;
	}

	public String getPlace() {
		return place;
	}

	public void setPlace(String place) {
		this.place = place;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
