package org.openmhealth.reference.request;

import java.net.URL;

import org.openmhealth.reference.data.ExternalAuthorizationInformationBin;
import org.openmhealth.reference.data.ExternalAuthorizationTokenBin;
import org.openmhealth.reference.domain.AuthenticationToken;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.ShimRegistry;

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
	 * Creates a request to determine if a user has authorized this server to
	 * read their data from some domain.
	 * 
	 * @param authToken
	 *        The user's authentication token.
	 * 
	 * @param domain
	 *        The domain for which authorization is in question.
	 * 
	 * @throws OmhException
	 *         One or more parameters were invalid.
	 */
	public UserAuthorizedDomainRequest(
		final AuthenticationToken authToken,
		final String domain)
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
			// Get the authorize URL.
			URL authorizeUrl = ShimRegistry.getShim(domain).getAuthorizeUrl();
			
			// Determine if this user has rejected the request in the past.
			boolean previouslyDenied =
				ExternalAuthorizationInformationBin
					.getInstance()
					.informationExists(authToken.getUsername(), domain);
			
			// Construct an object to return to the user.
			ExternalAuthorizationInformation information =
				new ExternalAuthorizationInformation(
					authToken.getUsername(),
					domain,
					authorizeUrl,
					previouslyDenied);
			
			// Store the object.
			ExternalAuthorizationInformationBin
				.getInstance()
				.storeInformation(information);
			
			// Set this request's data to the object.
			setData(information);
		}
	}
}