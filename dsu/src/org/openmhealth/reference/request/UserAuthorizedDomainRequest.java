package org.openmhealth.reference.request;

import java.io.IOException;
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
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openmhealth.reference.data.ExternalAuthorizationInformationBin;
import org.openmhealth.reference.data.ExternalAuthorizationTokenBin;
import org.openmhealth.reference.domain.AuthenticationToken;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation.RequestToken;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.Shim.AuthorizationMethod;
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
	 * The authentication token from the request.
	 */
	private final AuthenticationToken authToken;
	/**
	 * The domain in question.
	 */
	private final String domain;
	/**
	 * The root URL from the request that started this flow.
	 */
	private final String rootUrl;
	
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
	 * @param rootUrl
	 *        The root URL from the request. This will be used to create the
	 *        redirect URL for the OAuth flow.
	 * 
	 * @throws OmhException
	 *         One or more parameters were invalid.
	 */
	public UserAuthorizedDomainRequest(
		final AuthenticationToken authToken,
		final String domain,
		final String rootUrl)
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
		
		if(rootUrl == null) {
			throw new OmhException("The root URL is missing.");
		}
		else {
			this.rootUrl = rootUrl;
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
			
			// Get the authorize URL.
			URL authorizeUrl = shim.getAuthorizeUrl();
			
			// If OAuth v1, create the authorization header.
			RequestToken requestToken = null;
			if(AuthorizationMethod
				.OAUTH_1
				.equals(shim.getAuthorizationMethod())) {
				
				// Get the request token.
				requestToken = getRequestToken(shim);
				
				try {
					URIBuilder uriBuilder =
						new URIBuilder(authorizeUrl.toURI());
					uriBuilder
						.addParameter(
							RequestToken.OAUTH_PREFIX + "token",
							requestToken.getToken());
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
			}
			
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
					requestToken,
					previouslyDenied);
			
			// Store the object.
			ExternalAuthorizationInformationBin
				.getInstance()
				.storeInformation(information);
			
			// Set this request's data to the object.
			setData(information);
		}
	}
	
	/**
	 * Retrieves a request token for use in the OAuth v1 work-flow.
	 * 
	 * @param shim
	 *        The shim that contains the details regarding how to obtain such a
	 *        token.
	 * 
	 * @return The request token.
	 */
	private RequestToken getRequestToken(final Shim shim) throws OmhException {
		// Get the request URI.
		URI requestUri;
		try {
			requestUri = shim.getRequestTokenUrl().toURI();
		}
		catch(URISyntaxException e) {
			throw new OmhException("The request token URL was malformed.", e); 
		}
		
		// Build and URL-encode the required parameters.
		String
			callback,
			consumerKey,
			nonce,
			signatureMethod,
			timestamp,
			version;
		try {
			callback =
				URLEncoder
					.encode(
						rootUrl + AuthorizeDomainRequest.PATH,
						URL_ENCODING);
			consumerKey = URLEncoder.encode(shim.getClientId(), URL_ENCODING);
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
					callback +
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
					shim.getClientSecret().getBytes(),
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
			.append(callback)
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
		for(int i = 0; i < responseParts.length; i++) {
			// Process the current response part.
			String responsePart = responseParts[i];
			
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
}