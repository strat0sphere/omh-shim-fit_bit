package org.openmhealth.reference.domain;

import org.openmhealth.reference.exception.OmhException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
	 * The JSON key for the refresh token.
	 */
	public static final String JSON_KEY_REFRESH_TOKEN = "refresh_token";
	/**
	 * The JSON key for the time the token expires.
	 */
	public static final String JSON_KEY_EXPIRATION_TIME = "expiration_time";

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
	 * Creates a new authorization token based on the information from an
	 * existing authorization token, either one provided by the external party
	 * or by retrieving the information from the database.
	 * 
	 * @param username
	 *        The user-name of the Open mHealth user whose account is linked to
	 *        the external party.
	 * 
	 * @param accessToken
	 *        The token used to make requests to the external party.
	 * 
	 * @param refreshToken
	 *        The token used to generate new access and refresh tokens once
	 *        these have expired.
	 * 
	 * @param expiration
	 *        The time at which this access token expires.
	 * 
	 * @throws OmhException
	 *         A parameter was invalid.
	 */
	@JsonCreator
	public ExternalAuthorizationToken( 
		@JsonProperty(JSON_KEY_USERNAME) final String username,
		@JsonProperty(JSON_KEY_DOMAIN) final String domain,
		@JsonProperty(JSON_KEY_ACCESS_TOKEN) final String accessToken,
		@JsonProperty(JSON_KEY_REFRESH_TOKEN) final String refreshToken,
		@JsonProperty(JSON_KEY_EXPIRATION_TIME) final long expiration)
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
		if(refreshToken == null) {
			throw new OmhException("The refresh token is null.");
		}
		
		this.username = username;
		this.domain = domain;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expiration = expiration;
	}
}