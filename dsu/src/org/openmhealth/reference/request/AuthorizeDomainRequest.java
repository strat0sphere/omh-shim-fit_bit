package org.openmhealth.reference.request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.openmhealth.reference.data.ExternalAuthorizationInformationBin;
import org.openmhealth.reference.data.ExternalAuthorizationTokenBin;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.ShimRegistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>
 * This request handler is responsible for catching redirects from an external
 * party that is responding to an authorization verification by a user.
 * </p>
 *
 * @author John Jenkins
 */
public class AuthorizeDomainRequest extends Request<URL> {
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

			// Validate the authroize ID.
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
	 * <p>
	 * A representation of the expected return value from the token request.
	 * </p>
	 *
	 * @author John Jenkins
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Response {
		public static final String JSON_KEY_ACCESS_TOKEN = "access_token";
		public static final String JSON_KEY_REFRESH_TOKEN = "refresh_token";
		public static final String JSON_KEY_EXPIRES_IN = "expires_in";
		
		@JsonProperty(JSON_KEY_ACCESS_TOKEN)
		public final String accessToken;
		@JsonProperty(JSON_KEY_REFRESH_TOKEN)
		public final String refreshToken;
		@JsonProperty(JSON_KEY_EXPIRES_IN)
		public final long expiresIn;
		
		@JsonCreator
		public Response(
			@JsonProperty(JSON_KEY_ACCESS_TOKEN)
				final String accessToken,
			@JsonProperty(JSON_KEY_REFRESH_TOKEN)
				final String refreshToken,
			@JsonProperty(JSON_KEY_EXPIRES_IN)
				final long expiresIn)
			throws OmhException {
			
			// Validate the access token.
			if(accessToken == null) {
				throw new OmhException("The access token is null.");
			}
			else {
				this.accessToken = accessToken;
			}
			
			// Validate the refresh token.
			if(refreshToken == null) {
				throw new OmhException("The refresh token is null.");
			}
			else {
				this.refreshToken = refreshToken;
			}
			
			// Validate the expiration.
			this.expiresIn = expiresIn;
		}
	}
	
	/**
	 * The code generated by the external party, which should be exchanged for
	 * the information necessary to build an
	 * {@link ExternalAuthorizationToken}.
	 */
	private final String code;
	/**
	 * The state as supplied by the client.
	 */
	private final State state;
	
	/**
	 * Creates a request to handle an authorization response from an external
	 * party.
	 * 
	 * @param code
	 *        The code generated by the external party to exchange for the
	 *        information necessary to build an
	 *        {@link ExternalAuthorizationToken}.
	 * 
	 * @param state
	 *        The state as provided by the client.
	 * 
	 * @throws OmhException
	 *         A parameter was invalid.
	 */
	public AuthorizeDomainRequest(
		final String code,
		final State state)
		throws OmhException {
		
		// Validate the code.
		if(code == null) {
			throw new OmhException("The code is null.");
		}
		else {
			this.code = code;
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

		// Get the shim.
		Shim shim = ShimRegistry.getShim(information.getDomain());
		
		// Get the connection to the token URL.
		HttpPost request;
		try {
			request = new HttpPost(shim.getTokenUrl().toURI());
		}
		catch(URISyntaxException e) {
			throw new OmhException("The token URL was not a valid URL.", e);
		}
		
		// Add out secret as a header.
		request.addHeader("Authorization", "Basic " + shim.getClientSecret());
		
		// Add the parameters.
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(2);
        parameters
        	.add(new BasicNameValuePair("grant_type", "authorization_code"));
        parameters.add(new BasicNameValuePair("code", code));
        parameters
        	.add(new BasicNameValuePair("client_id", shim.getClientId()));
        try {
			request.setEntity(new UrlEncodedFormEntity(parameters));
		}
		catch(UnsupportedEncodingException e) {
			throw new OmhException("URL enconding is unknown.");
		}
        
        // Build a client to handle the request.
        HttpClient client = HttpClientBuilder.create().build();
        
        // Make the request and capture the response.
        HttpResponse response;
		try {
			response = client.execute(request);
		}
		catch(IOException e) {
			throw new OmhException("The token request failed.", e);
		}
        
        // Ensure the response succeeded.
        if(response.getStatusLine().getStatusCode() != 200) {
        	throw new OmhException("The token request failed.");
        }
        
        // Parse the response.
        ObjectMapper mapper = new ObjectMapper();
        Response responseObject;
		try {
			responseObject = mapper
        		.readValue(response.getEntity().getContent(), Response.class);
		}
		catch(JsonMappingException | JsonParseException e) {
			throw new OmhException("The response was not valid JSON.", e);
		}
		catch(IllegalStateException e) {
			throw new OmhException("The parser failed.", e);
		}
		catch(IOException e) {
			throw new OmhException("The response could not be read.", e);
		}
        
        // Build a token.
        ExternalAuthorizationToken token =
        	new ExternalAuthorizationToken(
        		information.getUsername(), 
        		information.getDomain(), 
        		responseObject.accessToken, 
        		responseObject.refreshToken, 
        		(responseObject.expiresIn - 1) * 1000);
		
		// Store the token.
        ExternalAuthorizationTokenBin.getInstance().storeToken(token);
		
		// Redirect the user.
        setData(state.clientUrl);
	}
}