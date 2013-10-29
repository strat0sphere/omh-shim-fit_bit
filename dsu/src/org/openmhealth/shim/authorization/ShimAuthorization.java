package org.openmhealth.shim.authorization;

import javax.servlet.http.HttpServletRequest;

import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.shim.Shim;

/**
 * <p>
 * The required authorization APIs to facilitate a user granting Open mHealth
 * authorization to their data. 
 * </p>
 *
 * @author John Jenkins
 */
public interface ShimAuthorization {
	/**
	 * Returns the information that can be used to initialize the authorization
	 * flow.
	 * 
	 * @param shim
	 *        The {@link Shim} to which this authorization request is related.
	 * 
	 * @param username
	 *        The user-name of the user about whom this request is related.
	 * 
	 * @param request
	 *        The HTTP request that was requesting the information needed to
	 *        submit a user to authorize Open mHealth to read data from this
	 *        Shim.
	 * 
	 * @return The information that can be used to initialize the authorization
	 *         flow.
	 */
	public ExternalAuthorizationInformation getAuthorizationInformation(
		final Shim shim,
		final String username,
		final HttpServletRequest request);
	
	/**
	 * Retrieves a new authorization token for use by the Shim when reading
	 * data. The token will be stored by the Open mHealth layer.
	 * 
	 * @param httpRequest
	 *        The HTTP callback request that was initiated after the user
	 *        granted (or denied) the request.
	 * 
	 * @param information
	 *        The {@link ExternalAuthorizationInformation} that was generated
	 *        when the client asked if the user had authorized Open mHealth to
	 *        read from this domain on their behalf.
	 * 
	 * @return An external authorization token to be stored by the Open mHealth
	 *         layer and given to the Shim when data is requested.
	 */
	public ExternalAuthorizationToken getAuthorizationToken(
		final HttpServletRequest httpRequest,
		final ExternalAuthorizationInformation information);
	
	/**
	 * Refreshes an existing authorization token and generates a new
	 * {@link ExternalAuthorizationToken} object, which will be stored by the
	 * Open mHealth layer. If the Shim's authorization tokens never expire,
	 * this can simply throw an {@link UnsupportedOperationException}.
	 * 
	 * @param oldToken
	 *        The current, expired token.
	 * 
	 * @return A newly-generated token to be used in the old token's place.
	 */
	public ExternalAuthorizationToken refreshAuthorizationToken(
		final ExternalAuthorizationToken oldToken);
}