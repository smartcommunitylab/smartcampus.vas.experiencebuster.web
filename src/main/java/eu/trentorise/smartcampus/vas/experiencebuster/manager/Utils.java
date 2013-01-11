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

import org.apache.commons.lang.ArrayUtils;

import eu.trentorise.smartcampus.ac.provider.model.User;

public class Utils {

	public static String userId(User user) {
		return user.getId() + "";
	}

	@SuppressWarnings(value = { "unchecked" })
	public static <T> T[] addElement(T[] collection, T element) {
		return (T[]) ArrayUtils.add(collection, element);
	}

	@SuppressWarnings(value = { "unchecked" })
	public static <T> T[] removeElement(T[] collection, T element) {
		return (T[]) ArrayUtils.removeElement(collection, element);
	}

	public static <T> boolean containsElement(T[] collection, T element) {
		return ArrayUtils.contains(collection, element);
	}

}
