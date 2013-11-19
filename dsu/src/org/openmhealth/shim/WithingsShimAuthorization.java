package org.openmhealth.shim;

import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.request.AuthorizeDomainRequest;
import org.openmhealth.reference.servlet.Version1;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.ShimRegistry;
import org.openmhealth.shim.authorization.ShimAuthorization;
import org.openmhealth.shim.authorization.oauth1.OAuth1Authorization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.http.HttpParameters;

import org.apache.commons.io.IOUtils;

public class WithingsShimAuthorization implements ShimAuthorization {
    private static final String OAUTH_URL_PREFIX = 
        "https://oauth.withings.com/account/";

    // The key used to store the Withings userid in the
    // ExternalAuthorizationToken extras map.
    public static final String KEY_EXTRAS_USERID = "userid";

	public ExternalAuthorizationInformation getAuthorizationInformation(
		final Shim shim,
		final String username,
		final URL clientRedirectUrl,
		final HttpServletRequest request) {
        WithingsShim withingsShim = (WithingsShim)shim;

		String authorizeId =
		    ExternalAuthorizationInformation.getNewAuthorizeId();
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Version1.PARAM_AUTHORIZATION_STATE, authorizeId);
		String callbackUrl = null;
        try {
            callbackUrl = 
                URLEncoder.encode(
                    AuthorizeDomainRequest.buildUrl(request, parameters),
                    "UTF-8");
        } 
        catch(UnsupportedEncodingException e) {
            throw new OmhException("URL encoding error", e);
        }

        // Fetch the request token.
        Map<String, String> requestTokenParameters = 
            new HashMap<String, String>();
        requestTokenParameters.put("oauth_callback", callbackUrl);

        InputStream tokenStream =
            ShimUtil.fetchUrl(
                withingsShim.buildSignedUrl(
                    OAUTH_URL_PREFIX + "request_token",
                    null, null, requestTokenParameters));

        // Parse the request token.
        Map <String, String> tokenParameters = parseTokenResponse(tokenStream);
        String token = tokenParameters.get(OAuth.OAUTH_TOKEN);
        String secret = tokenParameters.get(OAuth.OAUTH_TOKEN_SECRET);

        // Fill in the redirect URL.
        URL authorizeUrl =
            withingsShim.buildSignedUrl(
                OAUTH_URL_PREFIX + "authorize", token, secret, null);

        // Save the request token secret.
        Map<String, Object> preAuthState = new HashMap<String, Object>();
        preAuthState.put(OAuth1Authorization.KEY_EXTRAS_SECRET, secret);

		return
			new ExternalAuthorizationInformation(
				username,
				shim.getDomain(),
				authorizeId,
				authorizeUrl,
				clientRedirectUrl,
				preAuthState);
    }

	public ExternalAuthorizationToken getAuthorizationToken(
		final HttpServletRequest httpRequest,
		final ExternalAuthorizationInformation information) {
        // Find the shim.
        WithingsShim shim = null;
        try {
            shim = (WithingsShim)ShimRegistry.getShim(information.getDomain());
        }
        catch(Exception e) {
            throw new OmhException("Unable to get shim", e);
        }

        // Fetch the access token.
        String requestToken = httpRequest.getParameter("oauth_token");
        String requestTokenSecret = 
            (String)information.getPreAuthState().get(
                OAuth1Authorization.KEY_EXTRAS_SECRET);

        InputStream tokenStream =
            ShimUtil.fetchUrl(
                shim.buildSignedUrl(
                    OAUTH_URL_PREFIX + "access_token",
                    requestToken, requestTokenSecret, null));

        // Parse the access token.
        Map <String, String> tokenParameters = parseTokenResponse(tokenStream);
        String accessToken = tokenParameters.get(OAuth.OAUTH_TOKEN);
        String accessTokenSecret = 
            tokenParameters.get(OAuth.OAUTH_TOKEN_SECRET);

        // Construct and return the ExternalAuthorizationToken.
        Map<String, Object> extras = new HashMap<String, Object>();
        extras.put(
            OAuth1Authorization.KEY_EXTRAS_SECRET, 
            accessTokenSecret);
        extras.put(
            KEY_EXTRAS_USERID,
            httpRequest.getParameter("userid"));
        return new ExternalAuthorizationToken(
            information.getUsername(), information.getDomain(),
            accessToken, null, Long.MAX_VALUE, extras);
    }

	public ExternalAuthorizationToken refreshAuthorizationToken(
		final ExternalAuthorizationToken oldToken) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a token string returned by a token endpoint of the form
     * 'oauth_token=<token>&amp;oauth_token_secret=<secret>'.
     *
     * @param tokenStream
     *        The InputStream from an token HTTP request.
     *
     * @return A map containing a token and a secret using the keys
     *         OAuth.OAUTH_TOKEN and OAuth.OAUTH_TOKEN_SECRET.
     */
    private Map<String, String> parseTokenResponse(InputStream tokenStream) {
        String tokenString = null;
        try {
            tokenString = IOUtils.toString(tokenStream, "UTF-8");
        }
        catch(IOException e) {
            throw new OmhException("Error reading request token", e);
        }

        HttpParameters responseParams = OAuth.decodeForm(tokenString);

        Map<String, String> token = new HashMap<String, String>();
        token.put(
            OAuth.OAUTH_TOKEN,
            responseParams.getFirst(OAuth.OAUTH_TOKEN));
        token.put(
            OAuth.OAUTH_TOKEN_SECRET,
            responseParams.getFirst(OAuth.OAUTH_TOKEN_SECRET));

        return token;
    }
}
