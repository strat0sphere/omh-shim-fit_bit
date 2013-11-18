package org.openmhealth.reference.request;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openmhealth.reference.data.ExternalAuthorizationInformationBin;
import org.openmhealth.reference.data.ExternalAuthorizationTokenBin;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.reference.servlet.Version1;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.ShimRegistry;

/**
 * <p>
 * This request handler is responsible for catching redirects from an external
 * party that is responding to an authorization verification by a user.
 * </p>
 *
 * @author John Jenkins
 */
public class AuthorizeDomainRequest extends Request<String> {
	/**
	 * The path from the root for this request.
	 */
	public static final String PATH = "/auth/oauth/external_authorization";

	/**
	 * The HTTP callback request that was initialized after a user responded to
	 * an authorization request.
	 */
	private final HttpServletRequest httpRequest;
	/**
	 * The authorize ID to use to retrieve the
	 * {@link ExternalAuthorizationInformation} object.
	 */
	private final String authorizeId;

	/**
	 * Creates a request to handle an authorization response from an external
	 * party.
	 *
	 * @param httpRequest
	 *        The HTTP callback request from an external party.
	 *
	 * @param state
	 *        The state as provided by the client.
	 *
	 * @throws OmhException
	 *         A parameter was invalid.
	 */
	public AuthorizeDomainRequest(
		final HttpServletRequest httpRequest,
		final String state)
		throws OmhException {

		// Validate the code.
		if(httpRequest == null) {
			throw new OmhException("The HTTP request is null.");
		}
		else {
			this.httpRequest = httpRequest;
		}

		// Validate the state.
		if(state == null) {
			throw new OmhException("The state is null.");
		}
		else {
		    authorizeId = state;
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

		// Get the information that the client should have used when
		// redirecting the user.
		ExternalAuthorizationInformation information =
			ExternalAuthorizationInformationBin
				.getInstance()
				.getInformation(authorizeId);

		// Ensure that the client did use a known authorize ID.
		if(information == null) {
			throw new OmhException("The authorize ID is unknown.");
		}

		// Get the Shim.
		Shim shim = ShimRegistry.getShim(information.getDomain());

		// Create a new token.
		ExternalAuthorizationToken token =
			shim.getAuthorizationImplementation()
				.getAuthorizationToken(httpRequest, information);

		// Store the token.
        ExternalAuthorizationTokenBin.getInstance().storeToken(token);

		// Redirect the user.
        setData(information.getClientRedirectUrl().toString());
	}

	/**
     * Builds a URL for this request based on the incoming request.
     *
     * @param httpRequest
     *        The incoming HTTP request.
     *
     * @param parameters
     *        Additional parameters that should be attached to the URL. This
     *        may be null to indicate that no parameters should be added.
     *
     * @return A String representing the URL for this request.
     */
	public static String buildUrl(
	    final HttpServletRequest httpRequest,
	    final Map<String, String> parameters) {

	    // Build the base URL.
	    String result =
	        Version1.buildRootUrl(httpRequest) + Version1.PATH + PATH;

	    // If parameters were given, add them.
	    if(parameters != null) {
	        boolean firstPass = true;
	        for(String key : parameters.keySet()) {
	            if(firstPass) {
	                result += "?";
	                firstPass = false;
	            }
	            else {
	                result += "&";
	            }

	            try {
                    result += URLEncoder.encode(key, "UTF-8");
                    result += "=";
                    result += URLEncoder.encode(parameters.get(key), "UTF-8");
                }
                catch(UnsupportedEncodingException e) {
                    throw
                        new IllegalStateException(
                            "'UTF-8' is an unknown encoding.",
                            e);
                }
	        }
	    }

	    // Return the result.
		return result;
	}
}