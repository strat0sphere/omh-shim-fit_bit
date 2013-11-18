package org.openmhealth.shim.authorization.oauth1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.domain.OmhObject;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.reference.request.AuthorizeDomainRequest;
import org.openmhealth.reference.servlet.Version1;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.authorization.ShimAuthorization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>
 * The implementation of the authorization flow for OAuth v1. While this class
 * contains the majority of the business logic, subclasses must specify a few
 * fields that are specific to their Shim.
 * </p>
 *
 * @author John Jenkins
 */
public abstract class OAuth1Authorization implements ShimAuthorization {
	/**
	 * <p>
	 * The representation of the response from a request token request to an
	 * OAuth v1 entity.
	 * </p>
	 *
	 * @author John Jenkins
	 */
	public static class RequestToken implements OmhObject {
		/**
		 * The version of this class used for serialization purposes.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * The prefix for all OAuth parameters.
		 */
		public static final String OAUTH_PREFIX = "oauth_";

		/**
		 * The key for the token value.
		 */
		public static final String JSON_KEY_TOKEN =
			OAUTH_PREFIX + "token";
		/**
		 * The key for the secret value.
		 */
		public static final String JSON_KEY_SECRET =
			OAUTH_PREFIX + "token_secret";

		/**
		 * The token value.
		 */
		@JsonProperty(JSON_KEY_TOKEN)
		private String token;
		/**
		 * The corresponding secret.
		 */
		@JsonProperty(JSON_KEY_SECRET)
		private String secret;

		/**
		 * The default constructor builds an unverified object. Objects built
		 * this way should have {@link #verify()} called on them before they
		 * are used.
		 */
		public RequestToken() {
			// Do nothing.
		}

		/**
		 * Reconstructs an existing request token.
		 *
		 * @param token
		 *        The token's value.
		 *
		 * @param secret
		 *        The token's secret.
		 *
		 * @throws OmhException
		 *         The value or secret are null.
		 */
		@JsonCreator
		public RequestToken(
			@JsonProperty(JSON_KEY_TOKEN) final String token,
			@JsonProperty(JSON_KEY_SECRET) final String secret)
			throws OmhException {

			if(token == null) {
				throw new OmhException("The token is null.");
			}
			if(secret == null) {
				throw new OmhException("The secret is null.");
			}

			this.token = token;
			this.secret = secret;
		}

		/**
		 * Returns the token. This may be null if this has not been validated
		 * yet.
		 *
		 * @return The token, which may be null.
		 *
		 * @see validate()
		 */
		public String getToken() {
			return token;
		}

		/**
		 * Returns the secret associated with this token. This may be null if
		 * this has not been validated yet.
		 *
		 * @return The secret associated with this token, which may be null.
		 *
		 * @see validate()
		 */
		public String getSecret() {
			return secret;
		}

		/**
		 * Parses the key and value to determine if they need to be saved.
		 *
		 * @param key
		 *        The key from the response.
		 *
		 * @param value
		 *        The value associated with the key.
		 */
		public void parseParts(final String key, final String value) {
			// Determine which value this is and store it.
			if(JSON_KEY_TOKEN.equals(key)) {
				token = value;
			}
			else if(JSON_KEY_SECRET.equals(key)) {
				secret = value;
			}
		}

		/**
		 * Verifies that this object is properly built and, if not, throws an
		 * exception.
		 *
		 * @throws OmhException
		 *         This object is not properly built.
		 */
		public void verify() throws OmhException {
			if(token == null) {
				throw
					new OmhException(
						"The OAuth provider did not return a request token.");
			}
			if(secret == null) {
				throw
					new OmhException(
						"The OAuth provider did not return a request token " +
							"secret.");
			}
		}
	}

	/**
	 * The encoding to use for URL encoding.
	 */
	protected static final String URL_ENCODING = "UTF-8";

	/**
	 * The signature method for OAuth v1 request signing.
	 */
	protected static final String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";

	/**
	 * The internal key for the HMAC SHA1 algorithm.
	 */
	protected static final String HMAC_SHA1_NAME = "HmacSHA1";

	/**
	 * The OAuth v1 parameter key for a token, unauthorized or authorized.
	 */
	protected static final String OAUTH_TOKEN =
		RequestToken.OAUTH_PREFIX + "token";

	/**
	 * The extras key for the {@link RequestToken}.
	 */
	public static final String KEY_EXTRAS_TOKEN = "token";
	/**
	 * The extras key for the secret, which part of each token.
	 */
	public static final String KEY_EXTRAS_SECRET = "secret";

	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.shim.authorization.ShimAuthorization#getAuthorizationInformation(org.openmhealth.shim.Shim, java.lang.String, java.net.URL, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public ExternalAuthorizationInformation getAuthorizationInformation(
		final Shim shim,
		final String username,
		final URL clientRedirectUrl,
		final HttpServletRequest httpRequest)
		throws IllegalArgumentException {

		if(shim == null) {
			throw new IllegalArgumentException("The shim is null.");
		}
		if(username == null) {
			throw new IllegalArgumentException("The username is null.");
		}

		// Get this shim's domain.
		String domain = shim.getDomain();

		// Build the additional parameters that should be part of the callback
		// URL.
		String authorizeId =
		    ExternalAuthorizationInformation.getNewAuthorizeId();
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Version1.PARAM_AUTHORIZATION_STATE, authorizeId);

		// Build the callback URL.
		String callbackUrl =
		    AuthorizeDomainRequest.buildUrl(httpRequest, parameters);

		// Get the request token.
		RequestToken requestToken = getRequestToken(shim, callbackUrl);

        // Get the authorize URL.
        URL authorizeUrl = getAuthorizeUrl();

		// Build the authorize URL with the required parameters.
		try {
			URIBuilder uriBuilder =
				new URIBuilder(authorizeUrl.toURI());
			uriBuilder.addParameter(OAUTH_TOKEN, requestToken.getToken());
			authorizeUrl = uriBuilder.build().toURL();
		}
		catch(URISyntaxException e) {
			throw
				new OmhException(
					"The root authorize URL is invalid.",
					e);
		}
		catch(MalformedURLException e) {
			throw
				new OmhException(
					"The modified authorize URL is invalid.",
					e);
		}

		// Store the request token.
		Map<String, Object> preAuthState = new HashMap<String, Object>();
		preAuthState.put(KEY_EXTRAS_TOKEN, requestToken);

		// Construct an object to return to the user.
		return
			new ExternalAuthorizationInformation(
				username,
				domain,
				authorizeId,
				authorizeUrl,
				clientRedirectUrl,
				preAuthState);
	}

	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.shim.authorization.ShimAuthorization#getAuthorizationToken(javax.servlet.http.HttpServletRequest, org.openmhealth.reference.domain.ExternalAuthorizationInformation)
	 */
	@Override
	public ExternalAuthorizationToken getAuthorizationToken(
		final HttpServletRequest httpRequest,
		final ExternalAuthorizationInformation information)
		throws OmhException {

		// Get the request URL.
		URI tokenUri;
		try {
			tokenUri = getTokenUrl().toURI();
		}
		catch(URISyntaxException e) {
			throw new OmhException("The token URL was not a valid URL.", e);
		}

		// Get the connection to the token URL.
		HttpPost request = new HttpPost(tokenUri);

		// Attempt to get the request token object.
		Object requestTokenObject =
			information.getPreAuthState().get(KEY_EXTRAS_TOKEN);
		if(requestTokenObject == null) {
			throw
				new OmhException("The unauthorized request token is missing.");
		}
		RequestToken requestToken;
		if(requestTokenObject instanceof RequestToken) {
			requestToken = (RequestToken) requestTokenObject;
		}
		else {
			throw
				new OmhException(
					"The request token isn't a RequestToken object.");
		}

		// Build the request.
		buildTokenExchangeRequest(tokenUri, requestToken, request);

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
        return handleTokenExchangeResponse(information, response);
	}

	/**
	 * Not supported.
	 *
	 * @throws UnsupportedOperationException
	 *         The operation is not allowed.
	 */
	@Override
	public ExternalAuthorizationToken refreshAuthorizationToken(
		final ExternalAuthorizationToken oldToken) {

		throw
			new UnsupportedOperationException(
				"OAuth v1 tokens do not expire.");
	}

	/**
	 * Returns the URL for obtaining an unauthorized request token.
	 *
	 * @return The OAuth version 1 URL for obtaining an unauthorized request
	 *         token.
	 */
	public abstract URL getRequestTokenUrl();

	/**
	 * Returns the URL to redirect the user to authorize the unauthorized
	 * request token.
	 *
	 * @return The URL to the OAuth authorize end-point.
	 */
	public abstract URL getAuthorizeUrl();

	/**
	 * Returns the URL where a request token can be exchanged for an access
	 * token.
	 *
	 * @return The URL to the OAuth token end-point.
	 */
	public abstract URL getTokenUrl();

	/**
	 * Returns the shim's OAuth v1 consumer key for Open mHealth.
	 *
	 * @return The shim's OAuth v1 consumer key for Open mHealth.
	 */
	public abstract String getConsumerKey();

	/**
	 * Returns the shim's OAuth v1 consumer secret for Open mHealth.
	 *
	 * @return The shim's OAuth v1 consumer secret for Open mHealth.
	 */
	public abstract String getConsumerSecret();

	/**
	 * Retrieves a request token for use in the OAuth v1 work-flow.
	 *
	 * @param shim
	 *        The {@link Shim} that contains the details regarding how to
	 *        obtain such a token.
	 *
	 * @param rootUrl
	 *        The root URL that will be used to build the callback URL, which
	 *        must be included as part of the original request.
	 *
	 * @return The request token.
	 */
	private RequestToken getRequestToken(
		final Shim shim,
		final String callback)
		throws OmhException {

		// Get the request URI.
		URI requestUri;
		try {
			requestUri = getRequestTokenUrl().toURI();
		}
		catch(URISyntaxException e) {
			throw new OmhException("The request token URL was malformed.", e);
		}

		// Build and URL-encode the required parameters.
		String
		    encodedCallback,
			consumerKey,
			nonce,
			signatureMethod,
			timestamp,
			version;
		try {
		    encodedCallback = URLEncoder.encode(callback, URL_ENCODING);
			consumerKey = URLEncoder.encode(getConsumerKey(), URL_ENCODING);
			nonce =
				URLEncoder.encode(UUID.randomUUID().toString(), URL_ENCODING);
			signatureMethod =
				URLEncoder.encode(OAUTH_SIGNATURE_METHOD, URL_ENCODING);
			timestamp =
				URLEncoder
					.encode(
						Long.toString(
							System.currentTimeMillis() / 1000),
							URL_ENCODING);
			version = URLEncoder.encode("1.0", URL_ENCODING);
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
				URLEncoder.encode(requestUri.toString(), URL_ENCODING) + "&" +
				RequestToken.OAUTH_PREFIX +
					"callback" +
					URLEncoder.encode("=", URL_ENCODING) +
					encodedCallback +
					URLEncoder.encode("&", URL_ENCODING) +
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
					getConsumerSecret().getBytes(),
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
		StringBuilder authorizationHeaderbuilder = new StringBuilder("OAuth ");
		authorizationHeaderbuilder
			.append(RequestToken.OAUTH_PREFIX)
			.append("callback")
			.append('=')
			.append(encodedCallback)
			.append(',');
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
			.append("version")
			.append('=')
			.append(version);
		String authorizationHeader = authorizationHeaderbuilder.toString();

		// Create the request.
		HttpPost request = new HttpPost(requestUri);

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

		// Get the response body.
		String responseBody;
		try {
			Reader responseBodyReader =
				new InputStreamReader(response.getEntity().getContent());
			char[] buffer = new char[4096];
			int responseLength = responseBodyReader.read(buffer);
			responseBody = new String(buffer, 0, responseLength);
		}
		catch(IllegalStateException | IOException e) {
			throw
				new OmhException(
					"Could not read the response from the OAuth provider.",
					e);
		}

		// Parse out the token and secret.
		RequestToken token = new RequestToken();
		String[] responseParts = responseBody.split("&");
		for(String responsePart : responseParts) {
			// Break the response part into its two parts.
			String[] responsePartParts = responsePart.split("=", 2);

			// Ensure that this part contained an "=" sign.
			if(responsePartParts.length > 2) {
				continue;
			}

			// Get the key and value.
			String key = responsePartParts[0];
			String value = responsePartParts[1];

			// Pass the key and value to the token and let it handle them.
			token.parseParts(key, value);
		}

		// If all parts were not found, report an error.
		token.verify();

		// Return the token.
		return token;
	}

	/**
	 * Builds a request for an OAuth v1 provider.
	 *
	 * @param tokenUri
	 *        The pre-built URI for token exchange requests.
	 *
	 * @param requestToken
	 *        The unauthorized request token from the pre-authorization flow.
	 *
	 * @param request
	 *        The pre-built request to be modified by this builder.
	 */
	private void buildTokenExchangeRequest(
		final URI tokenUri,
		final RequestToken requestToken,
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
			consumerKey = URLEncoder.encode(getConsumerKey(), URL_ENCODING);
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
					requestToken.getToken().getBytes(),
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
	private ExternalAuthorizationToken handleTokenExchangeResponse(
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
		for(String responsePart : responseParts) {
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

		// Store the secret.
		Map<String, Object> extras = new HashMap<String, Object>();
		extras.put(KEY_EXTRAS_SECRET, secret);

        // Build and return a token.
        return
        	new ExternalAuthorizationToken(
        		information.getUsername(),
        		information.getDomain(),
        		token,
        		// Since OAuth doesn't use timeouts, we will store the secret
        		// as the refresh token.
        		null,
        		// The token does not expire, so this can be used indefinitely.
        		Long.MAX_VALUE,
        		// Add the extras.
        		extras);
	}
}