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

public class TwoNetShimAuthorization implements ShimAuthorization {
    // Keys used in the ExternalAuthorizationInformation::preAuthState and
    // ExternalAuthorizationToken#extras maps.

    // User GUID key.
    public static final String KEY_EXTRAS_USER = 
        "user";

    // Device GUID keys.
    public static final String KEY_EXTRAS_ENTRA_GLUCOMETER = 
        "entra_glucometer";
    public static final String KEY_EXTRAS_NONIN_PULSEOXIMETER = 
        "nonin_pulseoximeter";
    public static final String KEY_EXTRAS_AD_WEIGHT_SCALE = 
        "ad_weight_scale";
    public static final String KEY_EXTRAS_AD_BLOOD_PRESSURE = 
        "ad_blood_pressure";
    public static final String KEY_EXTRAS_ASTHMAPOLIS_SPIROSCOUT =
        "asthmapolis_spiroscout";

	public ExternalAuthorizationInformation getAuthorizationInformation(
		final Shim shim,
		final String username,
		final URL clientRedirectUrl,
		final HttpServletRequest request) {
        TwoNetShim twoNetShim = (TwoNetShim)shim;

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

        // Register a new user and one of each device and store the GUIDs in
        // the preAuthState map.
        Map<String, Object> preAuthState = new HashMap<String, Object>();

        String userGuid = twoNetShim.registerUser();
        preAuthState.put(KEY_EXTRAS_USER, userGuid);

        preAuthState.put(
            KEY_EXTRAS_ENTRA_GLUCOMETER,
            twoNetShim.registerDevice(
                userGuid, "Entra", "MGH-BT1", "2NET00001"));
        preAuthState.put(
            KEY_EXTRAS_NONIN_PULSEOXIMETER,
            twoNetShim.registerDevice(
                userGuid, "Nonin", "9560 Onyx II", "2NET00002"));
        preAuthState.put(
            KEY_EXTRAS_AD_WEIGHT_SCALE,
            twoNetShim.registerDevice(
                userGuid, "A&D", "UC-321PBT", "2NET00003"));
        preAuthState.put(
            KEY_EXTRAS_AD_BLOOD_PRESSURE,
            twoNetShim.registerDevice(
                userGuid, "A&D", "UA-767PBT", "2NET00004"));
        preAuthState.put(
            KEY_EXTRAS_ASTHMAPOLIS_SPIROSCOUT,
            twoNetShim.registerDevice(
                userGuid, "Asthmapolis", "Rev B", "2NET00005"));

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
