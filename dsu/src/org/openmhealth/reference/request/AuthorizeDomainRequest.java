package org.openmhealth.reference.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

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
import org.openmhealth.reference.domain.ExternalAuthorizationInformation.RequestToken;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.Shim.AuthorizationMethod;
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
	/**
	 * The path from the root for this request.
	 */
	public static final String PATH = "/auth/oauth/external_authorization";

	/**
	 * The encoding to use for URL encoding.
	 */
	private static final String URL_ENCODING = "UTF-8";
	/**
	 * The signature method for OAuth v1 request signing.
	 */
	private static final String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";
	
	/**
	 * The internal key for the HMAC SHA1 algorithm.
	 */
	private static final String HMAC_SHA1_NAME = "HmacSHA1";
	
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
		
		// Get the request URL.
		URI tokenUri;
		try {
			tokenUri = shim.getTokenUrl().toURI();
		}
		catch(URISyntaxException e) {
			throw new OmhException("The token URL was not a valid URL.", e);
		}
		
		// Get the connection to the token URL.
		HttpPost request = new HttpPost(tokenUri);
		
		// Get the authorization method.
		AuthorizationMethod authorizationMethod =
			shim.getAuthorizationMethod();
		
		// Build the request.
		// Check if we are dealing with OAuth v1.
		if(AuthorizationMethod.OAUTH_1.equals(authorizationMethod)) {
			buildOauth1(shim, tokenUri, information, request);
		}
		// Check if we are dealing with OAuth v2.
		else if(AuthorizationMethod.OAUTH_2.equals(authorizationMethod)) {
			buildOauth2(shim, request);
		}
		// Otherwise, indicate that we don't understand this authorization
		// method.
		else {
			throw new OmhException("The authorization method is unknown.");
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
        
        // Process the response and build a token.
        ExternalAuthorizationToken token = null;
		// Check if we are dealing with OAuth v1.
		if(AuthorizationMethod.OAUTH_1.equals(authorizationMethod)) {
			token = handleOauth1Response(information, response);
		}
		// Check if we are dealing with OAuth v2.
		else if(AuthorizationMethod.OAUTH_2.equals(authorizationMethod)) {
			token = handleOauth2Response(information, response);
		}
		
		// Store the token.
        ExternalAuthorizationTokenBin.getInstance().storeToken(token);
		
		// Redirect the user.
        setData(state.clientUrl);
	}
	
	/**
	 * Builds a request for an OAuth v1 provider.
	 * 
	 * @param shim
	 *        The shim that knows how to communicate with this provider.
	 * 
	 * @param tokenUri
	 *        The pre-built URI for token exchange requests.
	 * 
	 * @param information
	 *        The information built when the client was informed about the
	 *        user's desire to link accounts.
	 * 
	 * @param request
	 *        The pre-built request to be modified by this builder.
	 */
	private void buildOauth1(
		final Shim shim,
		final URI tokenUri,
		final ExternalAuthorizationInformation information,
		final HttpPost request) {
		
		// Build and URL-encode the required parameters.
		String
			consumerKey,
			nonce,
			signatureMethod,
			timestamp,
			token,
			version;
		try {
			consumerKey = URLEncoder.encode(shim.getClientId(), URL_ENCODING);
			nonce =
				URLEncoder
					.encode(UUID.randomUUID().toString(), URL_ENCODING);
			signatureMethod =
				URLEncoder.encode(OAUTH_SIGNATURE_METHOD, URL_ENCODING);
			timestamp =
				URLEncoder
					.encode(
						Long.toString(
							System.currentTimeMillis() / 1000),
							URL_ENCODING);
			version = URLEncoder.encode("1.0", URL_ENCODING);
			
			RequestToken requestToken = information.getRequestToken();
			if(requestToken == null) {
				throw
					new OmhException(
						"An OAuth v1 flow did not have a requset token.");
			}
			token =
				URLEncoder.encode(requestToken.getToken(), URL_ENCODING);
		}
		catch(UnsupportedEncodingException e) {
			throw
				new OmhException(
					"The encoding is unknown: " + URL_ENCODING,
					e);
		}
		
		// Build the raw signature.
		String rawSignature;
		try {
			rawSignature =
				"POST&" +
				URLEncoder.encode(tokenUri.toString(), URL_ENCODING) + "&" +
				RequestToken.OAUTH_PREFIX +
					"consumer_key" +
					URLEncoder.encode("=", URL_ENCODING) +
					consumerKey +
					URLEncoder.encode("&", URL_ENCODING) +
				RequestToken.OAUTH_PREFIX +
					"nonce" +
					URLEncoder.encode("=", URL_ENCODING) +
					nonce +
					URLEncoder.encode("&", URL_ENCODING) +
				RequestToken.OAUTH_PREFIX +
					"signature_method" +
					URLEncoder.encode("=", URL_ENCODING) +
					signatureMethod +
					URLEncoder.encode("&", URL_ENCODING) +
				RequestToken.OAUTH_PREFIX +
					"timestamp" +
					URLEncoder.encode("=", URL_ENCODING) +
					timestamp +
					URLEncoder.encode("&", URL_ENCODING) +
				RequestToken.OAUTH_PREFIX +
					"token" +
					URLEncoder.encode("=", URL_ENCODING) +
					token +
					URLEncoder.encode("&", URL_ENCODING) +
				RequestToken.OAUTH_PREFIX +
					"version" +
					URLEncoder.encode("=", URL_ENCODING) +
					version;
		}
		catch(UnsupportedEncodingException e) {
			throw
				new OmhException(
					"The encoding is unknown: " + URL_ENCODING,
					e);
		}
		
		// Hash the signature.
		String signature;
		try {
			SecretKeySpec signingKey =
				new SecretKeySpec(
					information.getRequestToken().getToken().getBytes(),
					HMAC_SHA1_NAME);
			Mac mac = Mac.getInstance(HMAC_SHA1_NAME);
			mac.init(signingKey);
			byte[] rawHmac = mac.doFinal(rawSignature.getBytes());
			signature =
				URLEncoder
					.encode(
						DatatypeConverter.printBase64Binary(rawHmac),
						URL_ENCODING);
		}
		catch(NoSuchAlgorithmException e) {
			throw new OmhException("The HMAC-SHA1 method is unknown.", e);
		}
		catch(InvalidKeyException e) {
			throw new OmhException("The key is invalid.", e);
		}
		catch(UnsupportedEncodingException e) {
			throw
				new OmhException(
					"The encoding is unknown: " + URL_ENCODING,
					e);
		}
		
		// Build the authorization header.
		StringBuilder authorizationHeaderbuilder =
			new StringBuilder("OAuth ");
		authorizationHeaderbuilder
			.append(RequestToken.OAUTH_PREFIX)
			.append("consumer_key")
			.append('=')
			.append(consumerKey)
			.append(',');
		authorizationHeaderbuilder
			.append(RequestToken.OAUTH_PREFIX)
			.append("nonce")
			.append('=')
			.append(nonce)
			.append(',');
		authorizationHeaderbuilder
			.append(RequestToken.OAUTH_PREFIX)
			.append("signature")
			.append('=')
			.append(signature)
			.append(',');
		authorizationHeaderbuilder
			.append(RequestToken.OAUTH_PREFIX)
			.append("signature_method")
			.append('=')
			.append(signatureMethod)
			.append(',');
		authorizationHeaderbuilder
			.append(RequestToken.OAUTH_PREFIX)
			.append("timestamp")
			.append('=')
			.append(timestamp)
			.append(',');
		authorizationHeaderbuilder
			.append(RequestToken.OAUTH_PREFIX)
			.append("token")
			.append('=')
			.append(token)
			.append(',');
		authorizationHeaderbuilder
			.append(RequestToken.OAUTH_PREFIX)
			.append("version")
			.append('=')
			.append(version);
		String authorizationHeader = authorizationHeaderbuilder.toString();
		
		// Add the OAuth header.
		request.addHeader("Authorization", authorizationHeader);
		
		// Create a client to make the request.
		HttpClient client = HttpClientBuilder.create().build();
		
		// Make the request.
		HttpResponse response;
		try {
			response = client.execute(request);
		}
		catch(IOException e) {
			throw
				new OmhException(
					"Could not get a key from the OAuth provider.",
					e);
		}
		
		// Make sure the request succeeded.
		if(response.getStatusLine().getStatusCode() != 200) {
			throw new OmhException("The request token request failed.");
		}
	}
	
	/**
	 * Handles a response from an OAuth v2 token exchange.
	 * 
	 * @param information
	 *        The information built when the client was informed about the
	 *        user's desire to link accounts.
	 * 
	 * @param response
	 *        The HTTP response to the token exchange request.
	 * 
	 * @return An ExternalAuthorizationToken to use for authorization.
	 * 
	 * @throws OmhException
	 *         There was a problem reading and/or parsing the response.
	 */
	private ExternalAuthorizationToken handleOauth1Response(
		final ExternalAuthorizationInformation information,
		final HttpResponse response)
		throws OmhException {
		
		// Get the response string.
		String responseString;
		try {
			InputStream input = response.getEntity().getContent();
			int amountRead;
			byte[] buffer = new byte[4096];
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			while((amountRead = input.read(buffer)) != -1) {
				output.write(buffer, 0, amountRead);
			}
			responseString = output.toString();
		}
		catch(IllegalStateException | IOException e) {
			throw
				new OmhException("Could not read the provider's response.", e);
		}
		
		// Parse out the token and secret.
		String token = null, secret = null;
		String[] responseParts = responseString.split("&");
		for(int i = 0; i < responseParts.length; i++) {
			// Get this part.
			String responsePart = responseParts[i];
			
			// Split this part into at most two pieces based on the equals
			// sign.
			String[] responsePartParts = responsePart.split("=", 2);
			
			// If there is only one piece, ignore this part.
			if(responsePartParts.length != 2) {
				continue;
			}

			// Parse out the key and value.
			String key = responsePartParts[0];
			String value = responsePartParts[1];
			
			// If the key is the token, save it.
			if((RequestToken.OAUTH_PREFIX + "token").equals(key)) {
				token = value;
			}
			// If the key is the secret, save it.
			else if((RequestToken.OAUTH_PREFIX + "token_secret").equals(key)) {
				secret = value;
			}
		}
		
		if(token == null) {
			throw new OmhException("A token was not returned.");
		}
		if(secret == null) {
			throw new OmhException("A secret was not returned.");
		}

        // Build and return a token.
        return
        	new ExternalAuthorizationToken(
        		information.getUsername(), 
        		information.getDomain(), 
        		token,
        		// Since OAuth doesn't use timeouts, we will store the secret
        		// as the refresh token.
        		secret,
        		// The token does not expire, so this can be used indefinitely.
        		Long.MAX_VALUE);
	}
	
	/**
	 * Builds a request for an OAuth v2 provider.
	 * 
	 * @param shim
	 *        The shim that knows how to communicate with this provider.
	 * 
	 * @param request
	 *        The pre-built request to be modified by this builder.
	 */
	private void buildOauth2(
		final Shim shim,
		final HttpPost request)
		throws OmhException {
		
		// Add our secret as a header.
		request
			.addHeader("Authorization", "Basic " + shim.getClientSecret());
		
		// Add the parameters.
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(2);
        parameters
        	.add(
        		new BasicNameValuePair(
        			"grant_type",
        			"authorization_code"));
        parameters.add(new BasicNameValuePair("code", code));
        parameters
        	.add(new BasicNameValuePair("client_id", shim.getClientId()));
        
        // Set the parameters as the request's entity.
        try {
			request.setEntity(new UrlEncodedFormEntity(parameters));
		}
		catch(UnsupportedEncodingException e) {
			throw new OmhException("URL enconding is unknown.");
		}
	}
	
	/**
	 * Handles a response from an OAuth v2 token exchange.
	 * 
	 * @param information
	 *        The information built when the client was informed about the
	 *        user's desire to link accounts.
	 * 
	 * @param response
	 *        The HTTP response to the token exchange request.
	 * 
	 * @return An ExternalAuthorizationToken to use for authorization.
	 * 
	 * @throws OmhException
	 *         There was a problem reading and/or parsing the response.
	 */
	private ExternalAuthorizationToken handleOauth2Response(
		final ExternalAuthorizationInformation information,
		final HttpResponse response)
		throws OmhException {
		
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
        
        // Build and return a token.
        return
        	new ExternalAuthorizationToken(
        		information.getUsername(), 
        		information.getDomain(), 
        		responseObject.accessToken, 
        		responseObject.refreshToken,
        		// We subtract a second to account for our own processing time.
        		// This decreases (although, not eliminates) the chance of a
        		// client using a token after it has expired.
        		(responseObject.expiresIn - 1) * 1000);
	}
}