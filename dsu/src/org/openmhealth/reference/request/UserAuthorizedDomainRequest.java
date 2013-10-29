package org.openmhealth.reference.request;

import javax.servlet.http.HttpServletRequest;

import org.openmhealth.reference.data.ExternalAuthorizationInformationBin;
import org.openmhealth.reference.data.ExternalAuthorizationTokenBin;
import org.openmhealth.reference.domain.AuthenticationToken;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.ShimRegistry;
import org.openmhealth.shim.authorization.ShimAuthorization;

/**
 * <p>
 * Determines if a user has authorized this server to read their data from some
 * domain.
 * </p>
 *
 * @author John Jenkins
 */
public class UserAuthorizedDomainRequest
	extends Request<ExternalAuthorizationInformation> {
	
	/**
	 * The path to this request's API.
	 */
	public static final String PATH = "auth/authorized";
	
	/**
	 * The authentication token from the request.
	 */
	private final AuthenticationToken authToken;
	/**
	 * The domain in question.
	 */
	private final String domain;
	/**
	 * The HTTP request that started this flow.
	 */
	private final HttpServletRequest httpRequest;
	
	/**
	 * Creates a request to determine if a user has authorized this server to
	 * read their data from some domain.
	 * 
	 * @param authToken
	 *        The user's authentication token.
	 * 
	 * @param domain
	 *        The domain for which authorization is in question.
	 * 
	 * @param httpRequest
	 *        The HTTP request from the client that is attempting to determine
	 *        if the user has already authorized Open mHealth to read from this
	 *        domain.
	 * 
	 * @throws OmhException
	 *         One or more parameters were invalid.
	 */
	public UserAuthorizedDomainRequest(
		final AuthenticationToken authToken,
		final String domain,
		final HttpServletRequest httpRequest)
		throws OmhException {
		
		if(authToken == null) {
			throw new OmhException("The authentication token is missing.");
		}
		else {
			this.authToken = authToken;
		}
		
		if(domain == null) {
			throw new OmhException("The domain is missing.");
		}
		else {
			this.domain = domain;
		}
		
		if(httpRequest == null) {
			throw new OmhException("The HTTP request is missing.");
		}
		else {
			this.httpRequest = httpRequest;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.request.Request#service()
	 */
	@Override
	public void service() throws OmhException {
		// First, short-circuit if this request has already been serviced.
		if(isServiced()) {
			return;
		}
		else {
			setServiced();
		}
		
		// First, ensure that the domain is known.
		if(! ShimRegistry.hasDomain(domain)) {
			throw new OmhException("The domain is unknown.");
		}
		
		// Attempt to get the latest token for the user in the domain.
		ExternalAuthorizationToken token =
			ExternalAuthorizationTokenBin
				.getInstance()
				.getToken(authToken.getUsername(), domain);
		
		// If no such token exists, add the URL to the response.
		if(token == null) {
			// Get the shim.
			Shim shim = ShimRegistry.getShim(domain);
			
			// Get the shim's authorization implementation.
			ShimAuthorization authorization =
				shim.getAuthorizationImplementation();
			
			// Use the implementation to generate information about how to
			// request a new token.
			ExternalAuthorizationInformation information =
				authorization
					.getAuthorizationInformation(
						shim,
						authToken.getUsername(),
						httpRequest);
			
			// Store the object.
			ExternalAuthorizationInformationBin
				.getInstance()
				.storeInformation(information);
			
			// Set this request's data to the object.
			setData(information);
		}
	}
}