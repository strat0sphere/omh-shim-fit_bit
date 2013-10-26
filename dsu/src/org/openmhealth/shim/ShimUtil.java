package org.openmhealth.shim;

import java.net.MalformedURLException;
import java.net.URL;

import org.openmhealth.shim.exception.ShimSchemaException;

public class ShimUtil {
    /**
     * Given a schema ID, returns the data type. For example, given
     * 'omh:fitbit:activity', will return 'activity'.
     */
    public static String dataTypeFromSchemaId(String id)
        throws ShimSchemaException {
        if (id == null) {
            throw new ShimSchemaException("id is null");
        }

        String[] parts = id.split(":");

        if (parts.length != 3) {
            throw new ShimSchemaException("Invalid schema id: " + id);
        }

        return parts[2];
    }

    /**
     * Creates a URL from a String.
     */
    public static URL buildURL(String urlStr) {
        URL url = null;

        try {
            url = new URL(urlStr);
        }
        catch(MalformedURLException e) {
            // Ignore the exception since all URLs are hardcoded.
        }

        return url;
    }
}
