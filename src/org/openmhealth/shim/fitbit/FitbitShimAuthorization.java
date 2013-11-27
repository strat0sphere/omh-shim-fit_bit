package org.openmhealth.shim.fitbit;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.reference.request.AuthorizeDomainRequest;
import org.openmhealth.reference.servlet.Version1;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.ShimRegistry;
import org.openmhealth.shim.authorization.ShimAuthorization;
import org.openmhealth.shim.authorization.oauth1.OAuth1Authorization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.client.LocalUserDetail;
import com.fitbit.api.model.APIResourceCredentials;

public class FitbitShimAuthorization implements ShimAuthorization {
	public ExternalAuthorizationInformation getAuthorizationInformation(
		final Shim shim,
		final String username,
		final URL clientRedirectUrl,
		final HttpServletRequest request) {
        FitbitShim fitbitShim = (FitbitShim)shim;

        // Build the callback URL.
		String authorizeId =
		    ExternalAuthorizationInformation.getNewAuthorizeId();
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Version1.PARAM_AUTHORIZATION_STATE, authorizeId);
		String callbackUrl =
		    AuthorizeDomainRequest.buildUrl(request, parameters);

        // Build the authorization URL.
        LocalUserDetail localUserDetail = new LocalUserDetail(username);
        String authorizationUrlString = null;
        try {
            authorizationUrlString =
                fitbitShim.getApiClientService()
                    .getResourceOwnerAuthorizationURL(
                        localUserDetail, callbackUrl);
        }
        catch(FitbitAPIException e) {
            throw new OmhException("Fitbit API error", e);
        }

        URL authorizationUrl = null;
        try {
            authorizationUrl = new URL(authorizationUrlString);
        }
        catch(MalformedURLException e) {
            throw new OmhException("The authorization URL is invalid.", e);
        }

        // Build the preAuthState with the temporary token secret.
        Map<String, Object> preAuthState = new HashMap<String, Object>();
        preAuthState.put(
            OAuth1Authorization.KEY_EXTRAS_SECRET,
            fitbitShim.getCredentialsCache()
                .getResourceCredentials(localUserDetail)
                .getTempTokenSecret());

		return
			new ExternalAuthorizationInformation(
				username,
				shim.getDomain(),
				authorizeId,
				authorizationUrl,
				clientRedirectUrl,
				preAuthState);
    }

	public ExternalAuthorizationToken getAuthorizationToken(
		final HttpServletRequest httpRequest,
		final ExternalAuthorizationInformation information) {
        FitbitShim shim = null;
        try {
            shim = (FitbitShim)ShimRegistry.getShim(information.getDomain());
        }
        catch(Exception e) {
            throw new OmhException("Unable to get shim", e);
        }

        // Save the temporary token data to the credentials cache.
        LocalUserDetail localUserDetail = 
            new LocalUserDetail(information.getUsername());

        APIResourceCredentials credentials =
            new APIResourceCredentials(
                information.getUsername(), 
                httpRequest.getParameter("oauth_token"),
                (String)information.getPreAuthState().get(
                    OAuth1Authorization.KEY_EXTRAS_SECRET));
        credentials.setTempTokenVerifier(
            httpRequest.getParameter("oauth_verifier"));

        shim.getCredentialsCache().saveResourceCredentials(
            localUserDetail, credentials);

        // Fetch the permanent token.
        try {
            shim.getApiClientService().getTokenCredentials(localUserDetail);
        } catch (FitbitAPIException e) {
            throw new OmhException(
                "Unable to finish authorization with Fitbit", e);
        }

        // Build the extras with the token secret.
        Map<String, Object> extras = new HashMap<String, Object>();
        extras.put(
            OAuth1Authorization.KEY_EXTRAS_SECRET, 
            credentials.getAccessTokenSecret());

        return new ExternalAuthorizationToken(
            information.getUsername(), information.getDomain(),
            credentials.getAccessToken(), null, Long.MAX_VALUE, extras);
    }

	public ExternalAuthorizationToken refreshAuthorizationToken(
		final ExternalAuthorizationToken oldToken) {
        throw new UnsupportedOperationException();
    }
}
