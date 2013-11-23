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
import org.openmhealth.shim.ShimRegistry;
import org.openmhealth.shim.authorization.ShimAuthorization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BlueButtonPlusShimAuthorization implements ShimAuthorization {
	public ExternalAuthorizationInformation getAuthorizationInformation(
		final Shim shim,
		final String username,
		final URL clientRedirectUrl,
		final HttpServletRequest request) {
        BlueButtonPlusShim blueButtonPlusShim = (BlueButtonPlusShim)shim;

        // Build the redirect URL. Since this whole auth flow is faked, we're
        // going to redirect directly to the callback URL.
		String authorizeId =
		    ExternalAuthorizationInformation.getNewAuthorizeId();
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Version1.PARAM_AUTHORIZATION_STATE, authorizeId);
        URL redirectUrl = null;
        try {
            redirectUrl =
                new URL(
                    AuthorizeDomainRequest.buildUrl(request, parameters));
        }
        catch(MalformedURLException e) {
            throw new OmhException("Error creating redirect URL", e);
        }

        Map<String, Object> preAuthState = new HashMap<String, Object>();

		return
			new ExternalAuthorizationInformation(
				username,
				shim.getDomain(),
				authorizeId,
                redirectUrl,
				clientRedirectUrl,
				preAuthState);
    }

	public ExternalAuthorizationToken getAuthorizationToken(
		final HttpServletRequest httpRequest,
		final ExternalAuthorizationInformation information) {
        // The whole flow is faked, so just create a fake
        // ExternalAuthorizationToken and pass the preAuthState directly to the
        // extras map.
        return new ExternalAuthorizationToken(
            information.getUsername(), information.getDomain(),
            "unused", null, Long.MAX_VALUE, information.getPreAuthState());
    }

	public ExternalAuthorizationToken refreshAuthorizationToken(
		final ExternalAuthorizationToken oldToken) {
        throw new UnsupportedOperationException();
    }
}
