package org.openmhealth.shim;

import java.io.UnsupportedEncodingException;
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
import org.openmhealth.shim.authorization.ShimAuthorization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.client.LocalUserDetail;

public class FitbitShimAuthorization implements ShimAuthorization {
	public ExternalAuthorizationInformation getAuthorizationInformation(
		final Shim shim,
		final String username,
		final HttpServletRequest request) {
        ExternalAuthorizationInformation info =
            new ExternalAuthorizationInformation(
                username, shim.getDomain(), null, null, null);

        FitbitShim fitbitShim = (FitbitShim)shim;

        Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put(
            AuthorizeDomainRequest.State.JSON_KEY_AUTHORIZE_ID, 
            info.getAuthorizeId());
        stateMap.put(
            AuthorizeDomainRequest.State.JSON_KEY_CLIENT_URL, 
            "http://openmhealth.org/");

        ObjectMapper objectMapper = new ObjectMapper();
        String stateJson = objectMapper.valueToTree(stateMap).toString();
        String encodedState = null;
        try {
            encodedState = URLEncoder.encode(stateJson, "UTF-8");
        } 
        catch(UnsupportedEncodingException e) {
            throw new OmhException("Encoding error", e);
        }

        String callbackUrl = 
            Version1.buildRootUrl(request) 
            + Version1.PATH
            + AuthorizeDomainRequest.PATH
            + "?state=" + encodedState;

        String authorizationUrlString = null;
        try {
            authorizationUrlString =
                fitbitShim.getApiClientService()
                    .getResourceOwnerAuthorizationURL(
                        new LocalUserDetail("-"), callbackUrl);
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

        info.setUrl(authorizationUrl);

        return info;
    }

	public ExternalAuthorizationToken getAuthorizationToken(
		final HttpServletRequest httpRequest,
		final ExternalAuthorizationInformation information) {
        return null;
    }

	public ExternalAuthorizationToken refreshAuthorizationToken(
		final ExternalAuthorizationToken oldToken) {
        return null;
    }
}
