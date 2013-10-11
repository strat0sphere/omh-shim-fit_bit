package org.openmhealth.reference.domain;

import java.net.URL;
import java.util.UUID;

import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.ShimRegistry;

import com.fasterxml.jackson.annotation.JsonCreator;
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
public class ExternalAuthorizationInformation implements OmhObject {
	/**
	 * The version of this class used for serialization purposes.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The JSON key for the user-name.
	 */
	public static final String JSON_KEY_USERNAME = "username";
	/**
	 * The JSON key for the domain.
	 */
	public static final String JSON_KEY_DOMAIN = "domain";
	/**
	 * The JSON key for the unique identifier.
	 */
	public static final String JSON_KEY_AUTHORIZE_ID = "authorize_id";
	/**
	 * The JSON key for the URL.
	 */
	public static final String JSON_KEY_URL = "url";
	/**
	 * The JSON key for the creation date.
	 */
	public static final String JSON_KEY_CREATION_DATE = "creation_date";
	/**
	 * The JSON key for the flag indicating if this request has previously been
	 * denied.
	 */
	public static final String JSON_KEY_PREVIOUSLY_DENIED =
		"previously_denied";
	
	/**
	 * The Open mHealth user-name of the user.
	 */
	@JsonProperty(JSON_KEY_USERNAME)
	private final String username;
	/**
	 * The domain to which this applies.
	 * 
	 * @see ShimRegistry
	 */
	@JsonProperty(JSON_KEY_DOMAIN)
	private final String domain;
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
	 * The date-time in Unix milliseconds when this object was created.
	 */
	@JsonProperty(JSON_KEY_CREATION_DATE)
	private final long creationDate;
	/**
	 * Whether or not a request from this user to the domain has ever been
	 * attempted and denied before.
	 */
	@JsonProperty(JSON_KEY_PREVIOUSLY_DENIED)
	private final boolean previouslyDenied;
	
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
	 * @param previouslyDenied
	 *        Whether or not a similar request has ever been made and the user
	 *        denied it.
	 * 
	 * @throws OmhException
	 *         A parameter was invalid.
	 */
	public ExternalAuthorizationInformation(
		final String username,
		final String domain,
		final URL url,
		final boolean previouslyDenied)
		throws OmhException {
		
		if(username == null) {
			throw new OmhException("The username is null.");
		}
		if(domain == null) {
			throw new OmhException("The domain is null.");
		}
		if(url == null) {
			throw new OmhException("The URL is null.");
		}

		this.username = username;
		this.domain = domain;
		this.authorizeId = UUID.randomUUID().toString();
		this.url = url;
		this.creationDate = System.currentTimeMillis();
		this.previouslyDenied = previouslyDenied;
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
	 * @param url
	 *        The URL to which the request should be made for the domain.
	 * 
	 * @param previouslyDenied
	 *        Whether or not a similar request has ever been made and the user
	 *        denied it.
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
		@JsonProperty(JSON_KEY_CREATION_DATE) final long creationDate,
		@JsonProperty(JSON_KEY_PREVIOUSLY_DENIED)
			final boolean previouslyDenied)
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
		this.creationDate = creationDate;
		this.previouslyDenied = previouslyDenied;
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
}