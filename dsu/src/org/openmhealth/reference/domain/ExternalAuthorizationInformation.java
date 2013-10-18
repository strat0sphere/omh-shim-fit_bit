package org.openmhealth.reference.domain;

import java.net.URL;
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
	 * <p>
	 * The representation of the response from a request token request to an
	 * OAuth v1 entity.
	 * </p>
	 *
	 * @author John Jenkins
	 */
	public static class RequestToken implements OmhObject {
		/**
		 * The version of this class used for serialization purposes.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * The prefix for all OAuth parameters.
		 */
		public static final String OAUTH_PREFIX = "oauth_";
		
		/**
		 * The key for the token value.
		 */
		public static final String JSON_KEY_TOKEN =
			OAUTH_PREFIX + "token";
		/**
		 * The key for the secret value.
		 */
		public static final String JSON_KEY_SECRET =
			OAUTH_PREFIX + "token_secret";
		
		/**
		 * The token value.
		 */
		@JsonProperty(JSON_KEY_TOKEN)
		private String token;
		/**
		 * The corresponding secret.
		 */
		@JsonProperty(JSON_KEY_SECRET)
		private String secret;
		
		/**
		 * The default constructor builds an unverified object. Objects built
		 * this way should have {@link #verify()} called on them before they
		 * are used.
		 */
		public RequestToken() {
			// Do nothing.
		}
		
		/**
		 * Reconstructs an existing request token.
		 * 
		 * @param token
		 *        The token's value.
		 * 
		 * @param secret
		 *        The token's secret.
		 * 
		 * @throws OmhException
		 *         The value or secret are null.
		 */
		@JsonCreator
		public RequestToken(
			@JsonProperty(JSON_KEY_TOKEN) final String token,
			@JsonProperty(JSON_KEY_SECRET) final String secret)
			throws OmhException {
			
			if(token == null) {
				throw new OmhException("The token is null.");
			}
			if(secret == null) {
				throw new OmhException("The secret is null.");
			}
			
			this.token = token;
			this.secret = secret;
		}
		
		/**
		 * Returns the token. This may be null if this has not been validated
		 * yet.
		 * 
		 * @return The token, which may be null.
		 * 
		 * @see validate()
		 */
		public String getToken() {
			return token;
		}
		
		/**
		 * Returns the secret associated with this token. This may be null if
		 * this has not been validated yet.
		 * 
		 * @return The secret associated with this token, which may be null.
		 * 
		 * @see validate()
		 */
		public String getSecret() {
			return secret;
		}
		
		/**
		 * Parses the key and value to determine if they need to be saved.
		 * 
		 * @param key
		 *        The key from the response.
		 * 
		 * @param value
		 *        The value associated with the key.
		 */
		public void parseParts(final String key, final String value) {
			// Determine which value this is and store it.
			if(JSON_KEY_TOKEN.equals(key)) {
				token = value;
			}
			else if(JSON_KEY_SECRET.equals(key)) {
				secret = value;
			}
		}
		
		/**
		 * Verifies that this object is properly built and, if not, throws an
		 * exception.
		 * 
		 * @throws OmhException
		 *         This object is not properly built.
		 */
		public void verify() throws OmhException {
			if(token == null) {
				throw
					new OmhException(
						"The OAuth provider did not return a request token.");
			}
			if(secret == null) {
				throw
					new OmhException(
						"The OAuth provider did not return a request token " +
							"secret.");
			}
		}
	}
	
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
	 * The JSON key for the unique identifier.
	 */
	public static final String JSON_KEY_AUTHORIZE_ID = "authorize_id";
	/**
	 * The JSON key for the URL.
	 */
	public static final String JSON_KEY_URL = "url";
	/**
	 * The JSON key for the Authorization header.
	 */
	public static final String JSON_KEY_REQUEST_TOKEN = "request_token";
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
	 * The Authorization header that the client should send along with the
	 * request. This is only used for OAuth v1.
	 */
	@JsonProperty(JSON_KEY_REQUEST_TOKEN)
	@JacksonFieldFilter(JACKSON_FILTER_GROUP_ID)
	private final RequestToken requestToken;
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
	 * @param requestToken
	 *        If OAuth v1 is being used, this is the request token that was
	 *        generated by the provider when the flow was started.
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
		final RequestToken requestToken,
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
		this.requestToken = requestToken;
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
	 * @param authorizeId
	 *        The unique identifier for this information.
	 * 
	 * @param url
	 *        The URL to which the request should be made for the domain.
	 * 
	 * @param requestToken
	 *        If OAuth v1 is being used, this is the request token that was
	 *        generated by the provider when the flow was started.
	 * 
	 * @param creationDate
	 *        The time-stamp of when this information was generated.
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
		@JsonProperty(JSON_KEY_REQUEST_TOKEN) final RequestToken requestToken,
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
		this.requestToken = requestToken;
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
	public RequestToken getRequestToken() {
		return requestToken;
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
	
	/**
	 * Returns whether or not the user previously denied this request.
	 * 
	 * @return Whether or not the user previously denied this request.
	 */
	public boolean wasPreviouslyDenied() {
		return previouslyDenied;
	}
}