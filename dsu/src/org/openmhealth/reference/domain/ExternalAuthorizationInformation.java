package org.openmhealth.reference.domain;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.reference.util.OmhObjectMapper;
import org.openmhealth.reference.util.OmhObjectMapper.JacksonFieldFilter;
import org.openmhealth.shim.ShimRegistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * <p>
 * Representation of requests to external entities for authorization for a user
 * to read data through Open mHealth.
 * </p>
 *
 * @author John Jenkins
 */
@JsonFilter(ExternalAuthorizationInformation.JACKSON_FILTER_GROUP_ID)
public class ExternalAuthorizationInformation implements OmhObject {
	/**
	 * The version of this class used for serialization purposes.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The group ID for the Jackson filter. This must be unique to our class,
	 * whatever the value is.
	 */
	protected static final String JACKSON_FILTER_GROUP_ID =
		"org.openmhealth.reference.domain.ExternalAuthorizationInformation";
	// Register this class with the Open mHealth object mapper.
	static {
		OmhObjectMapper.register(ExternalAuthorizationInformation.class);
	}
	
	/**
	 * The JSON key for the user-name.
	 */
	public static final String JSON_KEY_USERNAME = "username";
	/**
	 * The JSON key for the domain.
	 */
	public static final String JSON_KEY_DOMAIN = "domain";
	/**
	 * The JSON key for the state that was generated before the user was
	 * presented with an authorization request.
	 */
	public static final String JSON_KEY_PRE_AUTH_STATE = "pre_auth_state";
	/**
	 * The JSON key for the unique identifier.
	 */
	public static final String JSON_KEY_AUTHORIZE_ID = "authorize_id";
	/**
	 * The JSON key for the URL.
	 */
	public static final String JSON_KEY_URL = "url";
	/**
	 * The JSON key for the custom headers.
	 */
	public static final String JSON_KEY_HEADERS = "headers";
	/**
	 * The JSON key for the creation date.
	 */
	public static final String JSON_KEY_CREATION_DATE = "creation_date";
	
	/**
	 * The Open mHealth user-name of the user.
	 */
	@JsonProperty(JSON_KEY_USERNAME)
	@JacksonFieldFilter(JACKSON_FILTER_GROUP_ID)
	private final String username;
	
	/**
	 * The domain to which this applies.
	 * 
	 * @see ShimRegistry
	 */
	@JsonProperty(JSON_KEY_DOMAIN)
	@JacksonFieldFilter(JACKSON_FILTER_GROUP_ID)
	private final String domain;
	
	/**
	 * Domain-specific fields that are set in the early stages of the
	 * authorization flow. Examples of these may be authorization codes that
	 * will be exchanged later.
	 */
	@JsonProperty(JSON_KEY_PRE_AUTH_STATE)
	@JacksonFieldFilter(JACKSON_FILTER_GROUP_ID)
	private final Map<String, Object> preAuthState;
	
	/**
	 * The unique identifier for this information.
	 */
	@JsonProperty(JSON_KEY_AUTHORIZE_ID)
	private final String authorizeId;
	
	/**
	 * The URL to use to begin the authorization request.
	 */
	@JsonProperty(JSON_KEY_URL)
	@JsonSerialize(using = ToStringSerializer.class)
	private final URL url;
	
	/**
	 * Additional headers that should be attached to the redirect.
	 */
	@JsonProperty(JSON_KEY_HEADERS)
	private final Map<String, String> headers;
	
	/**
	 * The date-time in Unix milliseconds when this object was created.
	 */
	@JsonProperty(JSON_KEY_CREATION_DATE)
	private final long creationDate;
	
	/**
	 * Creates a new set of external authorization information. This should be
	 * used when a user may be creating a new authorization request.
	 * 
	 * @param username
	 *        The user-name of the Open mHealth user that may be making the
	 *        request.
	 * 
	 * @param domain
	 *        The domain to which the request will be made.
	 * 
	 * @param url
	 *        The URL to which the request should be made for the domain.
	 * 
	 * @param headers
	 *        Additional headers that should be attached to the redirect.
	 * 
	 * @param preAuthState
	 *        Values that may be set before the authorization is presented to
	 *        the user. These will be retained-server side and never sent.
	 * 
	 * @throws OmhException
	 *         A parameter was invalid.
	 */
	public ExternalAuthorizationInformation(
		final String username,
		final String domain,
		final URL url,
		final Map<String, String> headers,
		final Map<String, Object> preAuthState)
		throws OmhException {
		
		this(
			username, 
			domain, 
			UUID.randomUUID().toString(), 
			url, 
			headers, 
			preAuthState, 
			System.currentTimeMillis());
	}
	
	/**
	 * Recreates an existing set of external authorization information. This
	 * should be used when an existing set of information is being restored.
	 * 
	 * @param username
	 *        The user-name of the Open mHealth user that may be making the
	 *        request.
	 * 
	 * @param domain
	 *        The domain to which the request will be made.
	 * 
	 * @param authorizeId
	 *        The unique identifier for this information.
	 * 
	 * @param url
	 *        The URL to which the request should be made for the domain.
	 * 
	 * @param headers
	 *        Additional headers that should be attached to the redirect.
	 * 
	 * @param preAuthState
	 *        Values that may be set before the authorization is presented to
	 *        the user. These will be retained-server side and never sent.
	 * 
	 * @param creationDate
	 *        The time-stamp of when this information was generated.
	 * 
	 * @throws OmhException
	 *         A parameter was invalid.
	 */
	@JsonCreator
	public ExternalAuthorizationInformation(
		@JsonProperty(JSON_KEY_USERNAME) final String username,
		@JsonProperty(JSON_KEY_DOMAIN) final String domain,
		@JsonProperty(JSON_KEY_AUTHORIZE_ID) final String authorizeId,
		@JsonProperty(JSON_KEY_URL) final URL url,
		@JsonProperty(JSON_KEY_HEADERS) final Map<String, String> headers,
		@JsonProperty(JSON_KEY_PRE_AUTH_STATE)
			final Map<String, Object> preAuthState,
		@JsonProperty(JSON_KEY_CREATION_DATE) final long creationDate)
		throws OmhException {
		
		if(username == null) {
			throw new OmhException("The username is null.");
		}
		if(domain == null) {
			throw new OmhException("The domain is null.");
		}
		if(authorizeId == null) {
			throw new OmhException("The authorize ID is null.");
		}
		if(url == null) {
			throw new OmhException("The URL is null.");
		}
		
		this.username = username;
		this.domain = domain;
		this.authorizeId = authorizeId;
		this.url = url;
		this.headers =
			(headers == null) ?
				Collections.<String, String>emptyMap() :
				Collections.unmodifiableMap(headers);
		this.preAuthState =
			(preAuthState == null) ?
				Collections.<String, Object>emptyMap() :
				Collections.unmodifiableMap(preAuthState);
		this.creationDate = creationDate;
	}
	
	/**
	 * Returns the user-name.
	 * 
	 * @return The user-name.
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Returns the domain.
	 * 
	 * @return The domain.
	 */
	public String getDomain() {
		return domain;
	}
	
	/**
	 * Returns the unique identifier for this object.
	 * 
	 * @return The unique identifier for this object.
	 */
	public String getAuthorizeId() {
		return authorizeId;
	}
	
	/**
	 * Returns the URL to which this request should be made.
	 * 
	 * @return The URL to which this request should be made.
	 */
	public URL getUrl() {
		return url;
	}
	
	/**
	 * Returns the request token, however it may be null.
	 * 
	 * @return The request token, which may be null.
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}
	
	/**
	 * Returns the state generated by the authorization flow before the user
	 * was presented with an authorization request.
	 * 
	 * @return The state generated by the authorization flow before the user
	 *         was presented with an authorization request.
	 */
	public Map<String, Object> getPreAuthState() {
		return preAuthState;
	}
	
	/**
	 * Returns the number of milliseconds since the Unix epoch at which time
	 * this object was created.
	 * 
	 * @return The number of milliseconds since the Unix epoch at which time
	 *         this object was created.
	 */
	public long getCreationDate() {
		return creationDate;
	}
}