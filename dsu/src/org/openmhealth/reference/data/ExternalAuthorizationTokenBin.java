package org.openmhealth.reference.data;

import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;

/**
 * <p>
 * The collection of {@link ExternalAuthorizationToken}s.
 * </p>
 *
 * @author John Jenkins
 */
public abstract class ExternalAuthorizationTokenBin {
	/**
	 * The name of the DB document/table/whatever that contains the
	 * authorization tokens provided by external systems.
	 */
	public static final String DB_NAME = "external_authorization_token_bin";
	
	/**
	 * The instance of this ExternalAuthorizationTokenBin to use.
	 */
	private static ExternalAuthorizationTokenBin instance;
	
	/**
	 * Default constructor.
	 */
	protected ExternalAuthorizationTokenBin() {
		instance = this;
	}
	
	/**
	 * Returns the singular instance of this class.
	 * 
	 * @return The singular instance of this class.
	 */
	public static ExternalAuthorizationTokenBin getInstance() {
		return instance;
	}
	
	/**
	 * Stores an existing external authorization token.
	 * 
	 * @param token
	 *        The token to be saved.
	 * 
	 * @throws OmhException
	 *         The code is null.
	 */
	public abstract void storeToken(
		final ExternalAuthorizationToken token)
		throws OmhException;
	
	/**
	 * Retrieves the token for the user and domain with the latest expiration
	 * date.
	 * 
	 * @param username
	 *        The user's Open mHealth user-name.
	 * 
	 * @param domain
	 *        The domain to which the token applies.
	 * 
	 * @throws OmhException
	 *         A parameter is invalid.
	 */
	public abstract ExternalAuthorizationToken getToken(
		final String username,
		final String domain)
		throws OmhException;
}