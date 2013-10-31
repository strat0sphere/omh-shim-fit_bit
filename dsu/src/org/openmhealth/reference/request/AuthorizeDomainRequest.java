package org.openmhealth.reference.request;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.openmhealth.reference.data.ExternalAuthorizationInformationBin;
import org.openmhealth.reference.data.ExternalAuthorizationTokenBin;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.reference.servlet.Version1;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.ShimRegistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>
 * This request handler is responsible for catching redirects from an external
 * party that is responding to an authorization verification by a user.
 * </p>
 *
 * @author John Jenkins
 */
public class AuthorizeDomainRequest extends Request<URL> {
	/**
	 * The path from the root for this request.
	 */
	public static final String PATH = "/auth/oauth/external_authorization";
	
	/**
	 * <p>
	 * The expected state that should be supplied by the client after
	 * redirecting a user to authorize Open mHealth to read an external party's
	 * data for that user.
	 * </p>
	 * 
	 * @author John Jenkins
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class State {
		/**
		 * The JSON key for the authorize ID.
		 */
		public static final String JSON_KEY_AUTHORIZE_ID = "authorize_id";
		/**
		 * The JSON key for the client's URL.
		 */
		public static final String JSON_KEY_CLIENT_URL =
			"client_url";
		
		/**
		 * The ID that was given to the client when it asked about whether or
		 * not a user had authorized Open mHealth to read data from an external
		 * party. This should correlate with
		 * {@link ExternalAuthorizationInformation#JSON_KEY_AUTHORIZE_ID}.
		 */
		@JsonProperty(JSON_KEY_AUTHORIZE_ID)
		private final String authorizeId;
		/**
		 * The client-supplied URL for us to use to redirect the user back to
		 * the client.
		 */
		@JsonProperty(JSON_KEY_CLIENT_URL)
		private final URL clientUrl;
		
		/**
		 * Creates a new State object.
		 * 
		 * @param authorizeId
		 *        The ID that was given to the client when it asked if the user
		 *        had authorized Open mHealth to read their data from an
		 *        external entity. This should correlate with
		 *        {@link ExternalAuthorizationInformation#JSON_KEY_AUTHORIZE_ID}
		 *        .
		 * 
		 * @param clientUrl
		 *        The URL to use to redirect the user back to the client.
		 * 
		 * @throws OmhException
		 *         A parameter was invalid.
		 */
		@JsonCreator
		public State(
			@JsonProperty(JSON_KEY_AUTHORIZE_ID)
				final String authorizeId,
			@JsonProperty(JSON_KEY_CLIENT_URL)
				final URL clientUrl)
			throws OmhException {

			// Validate the authorize ID.
			if(authorizeId == null) {
				throw new OmhException("The authorize ID is null.");
			}
			else {
				this.authorizeId = authorizeId;
			}
			
			// Validate the client URL.
			if(clientUrl == null) {
				throw new OmhException("The client URL is null.");
			}
			else {
				this.clientUrl = clientUrl;
			}
		}
		
		/**
		 * Returns the ID that was given to the client when it asked if the
		 * user had authorized Open mHealth to read their data from an external
		 * entity.
		 * 
		 * @return The ID that was given to the client when it asked if the
		 *         user had authorized Open mHealth to read their data from an
		 *         external entity.
		 * 
		 * @see ExternalAuthorizationInformation#JSON_KEY_AUTHORIZE_ID
		 */
		public String getAuthorizeId() {
			return authorizeId;
		}
		
		/**
		 * Returns the URL to the client.
		 * 
		 * @return The URL to the client.
		 */
		public URL getClientUrl() {
			return clientUrl;
		}
	}
	
	/**
	 * The HTTP callback request that was initialized after a user responded to
	 * an authorization request.
	 */
	private final HttpServletRequest httpRequest;
	/**
	 * The state as supplied by the client.
	 */
	private final State state;
	
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
		final State state)
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
			this.state = state;
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
				.getInformation(state.getAuthorizeId());
		
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
        setData(state.clientUrl);
	}
	
	/**
	 * Builds a URL for this request based on the incoming request.
	 * 
	 * @param httpRequest
	 *        The incoming HTTP request.
	 * 
	 * @return A String representing the URL for this request.
	 */
	public static String buildUrl(final HttpServletRequest httpRequest) {
		return Version1.buildRootUrl(httpRequest) + Version1.PATH + PATH;
	}
}