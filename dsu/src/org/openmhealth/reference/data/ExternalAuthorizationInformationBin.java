package org.openmhealth.reference.data;

import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.exception.OmhException;

/**
 * <p>
 * The interface to the database-backed collection of information about
 * possible requests to external parties that users have made to allow Open
 * mHealth to read their data.
 * </p>
 *
 * @author John Jenkins
 */
public abstract class ExternalAuthorizationInformationBin {
	/**
	 * The name of the DB document/table/whatever that contains the information
	 * about authorization requests to external entities.
	 */
	public static final String DB_NAME =
		"external_authorization_information_bin";
	
	/**
	 * The instance of this AuthenticationTokenBin to use. 
	 */
	protected static ExternalAuthorizationInformationBin instance;
	
	/**
	 * Default constructor.
	 */
	protected ExternalAuthorizationInformationBin() {
		instance = this;
	}
	
	/**
	 * Returns the singular instance of this class.
	 * 
	 * @return The singular instance of this class.
	 */
	public static ExternalAuthorizationInformationBin getInstance() {
		return instance;
	}

	/**
	 * Stores new information about an authorization request to an external
	 * resource.
	 * 
	 * @param information
	 *        The information to be saved.
	 * 
	 * @throws OmhException
	 *         The information is null.
	 */
	public abstract void storeInformation(
		final ExternalAuthorizationInformation information)
		throws OmhException;
	
	/**
	 * Determines if a record exists for the given user in the given domain.
	 * 
	 * @param username
	 *        The Open mHealth user-name of this user.
	 * 
	 * @param domain
	 *        The name of the domain.
	 * 
	 * @return True if this user has information for this domain; false,
	 *         otherwise.
	 * 
	 * @throws OmhException
	 *         A parameter is null.
	 */
	public abstract boolean informationExists(
		final String username,
		final String domain)
		throws OmhException ;
	
	/**
	 * Returns the external authorization information for a specific
	 * authorization ID.
	 * 
	 * @param authorizeId
	 *        The unique identifier for the authorization request.
	 * 
	 * @return The external authorization information based on the ID or null
	 *         if the ID is unknown.
	 * 
	 * @throws OmhException
	 *         Multiple authorization information entities have the same ID.
	 */
	public abstract ExternalAuthorizationInformation getInformation(
		final String authorizeId)
		throws OmhException;
}