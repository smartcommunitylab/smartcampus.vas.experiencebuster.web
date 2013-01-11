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
import it.unitn.disi.sweb.webapi.model.entity.EntityBase;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

@Component
public class FileManager {

	@Value("${smartcampus.vas.web.socialengine.host}")
	private String socialHost;
	@Value("${smartcampus.vas.web.socialengine.port}")
	private int socialPort;

	private SCWebApiClient socialEngineClient;

	public FileManager() throws IOException {
		super();
	}

	@PostConstruct
	private void init() {
		socialEngineClient = SCWebApiClient.getInstance(Locale.ENGLISH,
				socialHost, socialPort);
	}

	private static final Logger logger = Logger.getLogger(FileManager.class);

	private long uploadFile(long ownerId, File file) throws WebApiException {
		EntityBase eb = socialEngineClient.readEntityBase(socialEngineClient
				.readUser(ownerId).getEntityBaseId());
		return socialEngineClient.uploadFile(file, eb.getLabel());
	}

	private void replaceFile(long socialId, File file) throws WebApiException {
		socialEngineClient.replaceFile(socialId, file);
	}

	public long upload(long ownerId, File file)
			throws ExperienceBusterException {
		try {
			return uploadFile(ownerId, file);
		} catch (Exception e) {
			logger.error("Exception uploading file", e);
			throw new ExperienceBusterException();
		}
	}

	public long updload(long ownerId, byte[] file)
			throws ExperienceBusterException {
		try {
			File temp = File.createTempFile("sc_file", null);
			temp.deleteOnExit();
			FileCopyUtils.copy(file, temp);
			long fid = uploadFile(ownerId, temp);
			try {
				temp.delete();
			} catch (SecurityException e) {
				logger.warn("Exception delete temporary file", e);
			}
			return fid;
		} catch (Exception e) {
			logger.error("Exception uploading file", e);
			throw new ExperienceBusterException();
		}
	}

	public byte[] download(long socialId) throws ExperienceBusterException {
		try {

			return FileCopyUtils.copyToByteArray(socialEngineClient
					.openStream(socialId));
		} catch (Exception e) {
			logger.error("Exception downloading file", e);
			throw new ExperienceBusterException();
		}

	}

	public boolean delete(long socialId) {
		try {
			return socialEngineClient.deleteFile(socialId);
		} catch (WebApiException e) {
			logger.error("Exception deleting file " + socialId, e);
			return false;
		}
	}

	public boolean replace(long socialId, File file) {
		try {
			replaceFile(socialId, file);
			return true;
		} catch (WebApiException e) {
			logger.error("Exception replacing file " + socialId, e);
			return false;
		}
	}

	public boolean replace(long socialId, byte[] file) {
		try {
			File temp = File.createTempFile("sc_file", null);
			temp.deleteOnExit();
			FileCopyUtils.copy(file, temp);
			replaceFile(socialId, temp);
			try {
				temp.delete();
			} catch (SecurityException e) {
				logger.warn("Exception delete temporary file", e);
			}
			return true;
		} catch (Exception e) {
			logger.error("Exception replacing file " + socialId, e);
			return false;
		}
	}

}
