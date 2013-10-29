package org.openmhealth.shim;

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import name.jenkins.paul.john.concordia.Concordia;

import org.openmhealth.reference.domain.Schema;
import org.openmhealth.shim.exception.ShimSchemaException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ShimUtil {
    /**
     * Splits a schema ID into an array of strings.
     */
    private static String[] splitSchemaId(String id) {
        if (id == null) {
            throw new ShimSchemaException("id is null");
        }

        String[] parts = id.split(":");

        if (parts.length != 3) {
            throw new ShimSchemaException("Invalid schema id: " + id);
        }

        return parts;
    }

    /**
     * Given a schema ID, returns the data type. For example, given
     * 'omh:fitbit:activity', will return 'activity'.
     *
     * @param id
     *        The schema ID.
     *
     * @return The data type.
     */
    public static String dataTypeFromSchemaId(String id)
        throws ShimSchemaException {
        return splitSchemaId(id)[2];
    }

    /**
     * Given a schema ID, returns the domain. For example, given
     * 'omh:fitbit:activity', will return 'fitbit'.
     *
     * @param id
     *        The schema ID.
     *
     * @return The domain.
     */
    public static String domainFromSchemaId(String id)
        throws ShimSchemaException {
        return splitSchemaId(id)[1];
    }

    /**
     * Creates a URL from a String.
     *
     * @param urlStr
     *        The URL as a string.
     *
     * @return The URL as a URL object.
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

    /**
     * Constructs a Schema object for a schema ID. Schemas are read from JSON
     * files found on the classpath. For example, the schema for version 1 of
     * 'omh:fitbit:activity' would be found in a file called
     * 'schema/fitbit/1/activity.json'.
     *
	 * @param id
	 *        The schema ID.
	 * 
	 * @param version
	 *        The schema version.
	 * 
	 * @return The schema for the given schema-ID version pair or null if the
	 *         pair is unknown.
	 * 
	 * @throws ShimSchemaException
	 *         The ID and/or version are null.
     */
    public static Schema getSchema(
        final String id,
        final Long version)
        throws ShimSchemaException {
        if (id == null) {
            throw new ShimSchemaException("The given schema ID is null.");
        }
        if (version == null) {
            throw new ShimSchemaException("The given schema version is null.");
        }

        String schemaResourcePath =
            "/schema/" + domainFromSchemaId(id) + "/" + version + "/"
            + dataTypeFromSchemaId(id) + ".json";

        // Load and parse the schema from the schema file.
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream schemaStream =
            ShimUtil.class.getClassLoader()
                .getResourceAsStream(schemaResourcePath);
        Concordia concordia = null;
        try {
            concordia = new Concordia(schemaStream);
        }
        catch(Exception e) {
            throw new ShimSchemaException("Error reading schema.", e);
        }
                
        return new Schema(id, 1, concordia);
    }

    /**
     * Retrieves a shim config value from the associated properties file. For a
     * shim with the domain 'fitbit', the properties file should be located on
     * the classpath at 'config/fitbit.properties'.
     *
     * @param domain
     *        The shim's domain.
     *
     * @param key
     *        The key of the config value.
     *
     * @return The config value or null if it was not found.
     */
    public static String getShimProperty(
        final String domain,
        final String key) {
        String propertiesResourcePath = "/config/" + domain + ".properties";
        InputStream propertiesStream =
            ShimUtil.class.getClassLoader()
                .getResourceAsStream(propertiesResourcePath);

        Properties properties = new Properties();
        try {
            properties.load(propertiesStream);
        }
        catch(IOException e) {
            return null;
        }

        return properties.getProperty(key);
    }
}
