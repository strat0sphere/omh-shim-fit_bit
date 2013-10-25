package org.openmhealth.reference.domain;

import java.util.Collections;
import java.util.Map;

import org.openmhealth.reference.exception.OmhException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>
 * A token supplied by an external entity to be used for querying data from
 * that entity.
 * </p>
 *
 * @author John Jenkins
 */
public class ExternalAuthorizationToken implements OmhObject {
	/**
	 * The version of this class used for serialization purposes.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The JSON key for the user-name of the user to whom this token belongs.
	 */
	public static final String JSON_KEY_USERNAME = "username";
	/**
	 * The JSON key for the domain to which this token applies.
	 */
	public static final String JSON_KEY_DOMAIN = "domain";
	/**
	 * The JSON key for the access token.
	 */
	public static final String JSON_KEY_ACCESS_TOKEN = "access_token";
	/**
	 * The JSON key for the access token secret.
	 */
	public static final String JSON_KEY_ACCESS_TOKEN_SECRET = 
        "access_token_secret";
	/**
	 * The JSON key for the refresh token.
	 */
	public static final String JSON_KEY_REFRESH_TOKEN = "refresh_token";
	/**
	 * The JSON key for the time the token expires.
	 */
	public static final String JSON_KEY_EXPIRATION_TIME = "expiration_time";
	/**
	 * The JSON key for the extra information stored with this token.
	 */
	public static final String JSON_KEY_EXTRAS = "extras";

	/**
	 * The user-name of the user that owns this token.
	 */
	@JsonProperty(JSON_KEY_USERNAME)
	private final String username;
	/**
	 * The domain to which this token applies.
	 */
	@JsonProperty(JSON_KEY_DOMAIN)
	private final String domain;
	/**
	 * The access token to use for making requests.
	 */
	@JsonProperty(JSON_KEY_ACCESS_TOKEN)
	private final String accessToken;
	/**
	 * The access token secret to use for making requests.
	 */
	@JsonProperty(JSON_KEY_ACCESS_TOKEN_SECRET)
	private final String accessTokenSecret;
	/**
	 * The refresh token to use to get new access and refresh tokens when these
	 * expire.
	 */
	@JsonProperty(JSON_KEY_REFRESH_TOKEN)
	private final String refreshToken;
	/**
	 * The time in milliseconds when this token expires.
	 */
	@JsonProperty(JSON_KEY_EXPIRATION_TIME)
	private final long expiration;
	/**
	 * Domain-specific fields that should be stored with this token.
	 */
	@JsonProperty(JSON_KEY_EXTRAS)
	private final Map<String, Object> extras;
	
	/**
	 * Creates a new authorization token based on the information from an
	 * existing authorization token, either one provided by the external party
	 * or by retrieving the information from the database.
	 * 
	 * @param username
	 *        The user-name of the Open mHealth user whose account is linked to
	 *        the external party.
	 * 
	 * @param domain
	 *        The domain to which this token applies.
	 * 
	 * @param accessToken
	 *        The token used to make requests to the external party.
	 * 
	 * @param refreshToken
	 *        The token used to generate new access and refresh tokens once
	 *        these have expired, which may be null if this domain does not
	 *        refresh its tokens.
	 * 
	 * @param expiration
	 *        The time at which this access token expires. For tokens that do
	 *        not expire, this can simply be set to {@link Long#MAX_VALUE}.
	 * 
	 * @param extras
	 *        Domain-specific information stored with this token.
	 * 
	 * @throws OmhException
	 *         A parameter was invalid.
	 */
	@JsonCreator
	public ExternalAuthorizationToken( 
		@JsonProperty(JSON_KEY_USERNAME) final String username,
		@JsonProperty(JSON_KEY_DOMAIN) final String domain,
		@JsonProperty(JSON_KEY_ACCESS_TOKEN) final String accessToken,
		@JsonProperty(JSON_KEY_ACCESS_TOKEN_SECRET) 
        final String accessTokenSecret,
		@JsonProperty(JSON_KEY_REFRESH_TOKEN) final String refreshToken,
		@JsonProperty(JSON_KEY_EXPIRATION_TIME) final long expiration,
		@JsonProperty(JSON_KEY_EXTRAS) final Map<String, Object> extras)
		throws OmhException {

		if(username == null) {
			throw new OmhException("The username is null.");
		}
		if(domain == null) {
			throw new OmhException("The domain is null.");
		}
		if(accessToken == null) {
			throw new OmhException("The access token is null.");
		}
		if(accessTokenSecret == null && refreshToken == null) {
			throw new OmhException(
                "The access token secret and refresh token are both null.");
		}
		
		this.username = username;
		this.domain = domain;
		this.accessToken = accessToken;
		this.accessTokenSecret = accessTokenSecret;
		this.refreshToken = refreshToken;
		this.expiration = expiration;
		this.extras =
			(extras == null) ?
				Collections.<String, Object>emptyMap() :
				Collections.unmodifiableMap(extras);
	}
	
	/**
	 * Returns the Open mHealth user-name of the user to whom this token
	 * belongs.
	 * 
	 * @return The Open mHealth user-name of the user to whom this token
	 *         belongs.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Returns the domain to which this token is associated.
	 * 
	 * @return The domain to which this token is associated.
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * Returns the token to use to request data.
	 * 
	 * @return The token to use to request data.
	 */
	public String getAccessToken() {
		return accessToken;
	}

	/**
	 * Returns the token secret to use to request data.
	 * 
	 * @return The token secret to use to request data.
	 */
	public String getAccessTokenSecret() {
		return accessTokenSecret;
	}

	/**
	 * Returns the token to exchange for a new access and refresh token.
	 * 
	 * @return The token to exchange for a new access and refresh token.
	 */
	public String getRefreshToken() {
		return refreshToken;
	}

	/**
	 * Returns the date and time that this token expires, represented as the
	 * number of milliseconds since the Unix epoch.
	 * 
	 * @return The number of milliseconds since the Unix epoch at which time
	 *         this token expires.
	 */
	public long getExpiration() {
		return expiration;
	}
	
	/**
	 * Returns a domain-specific field.
	 * 
	 * @return A domain-specific field.
	 * 
	 * @throws OmhException
	 *         The key is unknown or the value is not the parameterized type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getExtra(final String key) {
		Object extra = extras.get(key);
		if(extra == null) {
			throw new OmhException("The extra value is unknown: " + key);
		}
		
		try {
			return (T) extra;
		}
		catch(ClassCastException e) {
			throw
				new OmhException(
					"The extra value is not the correct type: " +
						extra.getClass().toString());
		}
	}
	
	/**
	 * Returns whether or not the expiration token is still valid. There may be
	 * many reasons why it is invalid, so, if a specific check is desired e.g.
	 * the token has expired, a specific method, e.g. {@link #getExpiration()}
	 * should be used instead.
	 * 
	 * @return Whether or not this token is valid.
	 */
	public boolean isValid() {
		return (expiration < System.currentTimeMillis());
	}
}