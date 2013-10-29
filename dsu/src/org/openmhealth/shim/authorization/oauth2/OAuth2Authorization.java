package org.openmhealth.shim.authorization.oauth2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.authorization.ShimAuthorization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>
 * The implementation of the authorization flow for OAuth v2. While this class
 * contains the majority of the business logic, subclasses must specify a few
 * fields that are specific to their Shim.
 * </p>
 *
 * @author John Jenkins
 */
public abstract class OAuth2Authorization implements ShimAuthorization {
	/**
	 * <p>
	 * A representation of the expected return value from the token request.
	 * </p>
	 *
	 * @author John Jenkins
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Response {
		/**
		 * The JSON key for the access token.
		 */
		public static final String JSON_KEY_ACCESS_TOKEN = "access_token";
		/**
		 * The JSON key for the refresh token.
		 */
		public static final String JSON_KEY_REFRESH_TOKEN = "refresh_token";
		/**
		 * The JSON key for when the token expires.
		 */
		public static final String JSON_KEY_EXPIRES_IN = "expires_in";
		
		/**
		 * The access token to use when requesting data.
		 */
		@JsonProperty(JSON_KEY_ACCESS_TOKEN)
		public final String accessToken;
		/**
		 * The refresh token to use to get new access and refresh tokens.
		 */
		@JsonProperty(JSON_KEY_REFRESH_TOKEN)
		public final String refreshToken;
		/**
		 * The number of seconds when this token will expire.
		 */
		@JsonProperty(JSON_KEY_EXPIRES_IN)
		public final long expiresIn;
		
		/**
		 * Builds a response object, presumably using Jackson.
		 * 
		 * @param accessToken
		 *        The access token to use when requesting data.
		 * 
		 * @param refreshToken
		 *        The refresh token to use to get a new token.
		 * 
		 * @param expiresIn
		 *        The number of seconds before this token expires.
		 * 
		 * @throws OmhException
		 *         A required field is missing.
		 */
		@JsonCreator
		public Response(
			@JsonProperty(JSON_KEY_ACCESS_TOKEN) final String accessToken,
			@JsonProperty(JSON_KEY_REFRESH_TOKEN) final String refreshToken,
			@JsonProperty(JSON_KEY_EXPIRES_IN) final Long expiresIn)
			throws OmhException {
			
			// Validate the access token.
			if(accessToken == null) {
				throw new OmhException("The access token is null.");
			}
			else {
				this.accessToken = accessToken;
			}
			
			// Get the refresh token.
			this.refreshToken = refreshToken;
			
			// Get the expiration.
			this.expiresIn = expiresIn;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.shim.authorization.ShimAuthorization#getAuthorizationInformation(org.openmhealth.shim.Shim, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public ExternalAuthorizationInformation getAuthorizationInformation(
		final Shim shim,
		final String username,
		final HttpServletRequest request)
		throws IllegalArgumentException {
		
		if(shim == null) {
			throw new IllegalArgumentException("The shim is null.");
		}
		if(username == null) {
			throw new IllegalArgumentException("The username is null.");
		}
		
		// Get this shim's domain.
		String domain = shim.getDomain();
		
		// Get the authorize URL.
		URL authorizeUrl = getAuthorizeUrl();
		
		// Construct an object to return to the user.
		return
			new ExternalAuthorizationInformation(
				username,
				domain,
				authorizeUrl,
				null,
				null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.shim.authorization.ShimAuthorization#getAuthorizationToken(javax.servlet.http.HttpServletRequest, org.openmhealth.reference.domain.ExternalAuthorizationInformation)
	 */
	@Override
	public ExternalAuthorizationToken getAuthorizationToken(
		final HttpServletRequest httpRequest,
		final ExternalAuthorizationInformation information) {
		
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
		
		// Add our secret as a header.
		request.addHeader("Authorization", "Basic " + getClientSecret());
		
		// Retrieve the code from the request.
		String code = httpRequest.getParameter("code");
		
		// Add the parameters.
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(2);
        parameters
        	.add(
        		new BasicNameValuePair(
        			"grant_type",
        			"authorization_code"));
        parameters.add(new BasicNameValuePair("code", code));
        parameters
        	.add(new BasicNameValuePair("client_id", getClientId()));
        
        // Set the parameters as the request's entity.
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
			responseObject =
				mapper
        			.readValue(
        				response.getEntity().getContent(),
        				Response.class);
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
        		(responseObject.expiresIn - 1) * 1000,
        		null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.shim.authorization.ShimAuthorization#refreshAuthorizationToken(org.openmhealth.reference.domain.ExternalAuthorizationToken)
	 */
	@Override
	public ExternalAuthorizationToken refreshAuthorizationToken(
		final ExternalAuthorizationToken oldToken) {
		
		// Get the request URL.
		URI refreshUri;
		try {
			refreshUri = getRefreshUrl().toURI();
		}
		catch(URISyntaxException e) {
			throw new OmhException("The refresh URL was not a valid URL.", e);
		}
		
		// Get the connection to the refresh URL.
		HttpPost request = new HttpPost(refreshUri);
		
		// Add our secret as a header.
		request.addHeader("Authorization", "Basic " + getClientSecret());
		
		// Add the parameters.
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(2);
        parameters
        	.add(
        		new BasicNameValuePair(
        			"grant_type",
        			"refresh_token"));
        parameters
        	.add(
        		new BasicNameValuePair(
        			"refresh_token",
        			oldToken.getRefreshToken()));
        
        // Set the parameters as the request's entity.
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
        	throw new OmhException("The token refresh request failed.");
        }
        
        // Parse the response.
        ObjectMapper mapper = new ObjectMapper();
        Response responseObject;
		try {
			responseObject =
				mapper
        			.readValue(
        				response.getEntity().getContent(),
        				Response.class);
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
        		oldToken.getUsername(), 
        		oldToken.getDomain(), 
        		responseObject.accessToken, 
        		responseObject.refreshToken,
        		// We subtract a second to account for our own processing time.
        		// This decreases (although, not eliminates) the chance of a
        		// client using a token after it has expired.
        		(responseObject.expiresIn - 1) * 1000,
        		null);
	}
	
	/**
	 * Returns the URL to redirect the user to begin the authorization flow.
	 * 
	 * @return The URL to the OAuth v2 authorize end-point.
	 */
	public abstract URL getAuthorizeUrl();
	
	/**
	 * Returns the OAuth v2 URL where an access code can be exchanged for an
	 * access token.
	 * 
	 * @return The URL to the OAuth v2 token end-point.
	 */
	public abstract URL getTokenUrl();
	
	/**
	 * Returns the OAuth URL where a refresh token can be exchanged for a new
	 * access token and refresh token.
	 * 
	 * @return The URL to the OAuth v2 refresh end-point.
	 */
	public abstract URL getRefreshUrl();
	
	/**
	 * Returns the shim's OAuth v2 client ID for Open mHealth.
	 * 
	 * @return The shim's OAuth v2 client ID for Open mHealth.
	 */
	public abstract String getClientId();
	
	/**
	 * Returns the shim's OAuth v2 client secret for Open mHealth.
	 * 
	 * @return The shim's OAuth v2 client secret for Open mHealth.
	 */
	public abstract String getClientSecret();
}