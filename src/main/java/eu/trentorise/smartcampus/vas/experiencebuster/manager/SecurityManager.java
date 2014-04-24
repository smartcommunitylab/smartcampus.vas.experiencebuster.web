package eu.trentorise.smartcampus.vas.experiencebuster.manager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import eu.trentorise.smartcampus.aac.AACException;
import eu.trentorise.smartcampus.aac.AACService;
import eu.trentorise.smartcampus.aac.model.TokenData;

public class SecurityManager extends AACService {

	private static final Logger logger = Logger
			.getLogger(SecurityManager.class);
	private String clientId;
	private String clientSecret;
	private String securityProviderUrl;

	private String securityToken;
	private long expirationTime;

	// /** Timeout (in ms) we specify for each http request */
	// public static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

	public SecurityManager(String securityProviderUrl, String clientId,
			String clientSecret) {
		super(securityProviderUrl, clientId, clientSecret);
		if (securityProviderUrl != null && !securityProviderUrl.endsWith("/")) {
			securityProviderUrl += "/";
		}
		this.securityProviderUrl = securityProviderUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	public String getSecurityToken() throws SecurityException, AACException {
		if (securityToken == null
				|| System.currentTimeMillis() > expirationTime) {
			securityToken = requestToken();
			logger.info("Generated a security token");
		}

		// else if (System.currentTimeMillis() > expirationTime) {
		// logger.info("Refresh security token");
		// TokenData newToken;
		// newToken = requestToken(); //refreshToken(securityToken);
		// securityToken = newToken.getRefresh_token();
		// expirationTime = newToken.getExpires_on();
		// }
		return securityToken;
	}

	public String requestToken() {
		final HttpResponse resp;
		final HttpEntity entity = null;
		String url = securityProviderUrl
				+ "oauth/token?grant_type=client_credentials&client_id="
				+ clientId + "&client_secret=" + clientSecret;
		final HttpPost post = new HttpPost(url);
		post.setEntity(entity);
		post.setHeader("Accept", "application/json");
		try {
			resp = getHttpClient().execute(post);
			final String response = EntityUtils.toString(resp.getEntity());
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				TokenData data = TokenData.valueOf(response);
				securityToken = data.getAccess_token();
				expirationTime = data.getExpires_on();
				return securityToken;
			}
			throw new AACException("Error validating " + resp.getStatusLine());
		} catch (final Exception e) {
			return null;
		}
	}

	// protected static HttpClient getHttpClient() {
	// HttpClient httpClient = new DefaultHttpClient();
	// final HttpParams params = httpClient.getParams();
	// HttpConnectionParams.setConnectionTimeout(params,
	// HTTP_REQUEST_TIMEOUT_MS);
	// HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
	// ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
	// return httpClient;
	// }
}
