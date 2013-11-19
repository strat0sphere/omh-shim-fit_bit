package org.openmhealth.shim;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import name.jenkins.paul.john.concordia.Concordia;

import org.openmhealth.reference.domain.Schema;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.exception.ShimSchemaException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ShimUtil {
    /**
     * Splits a schema ID into an array of strings.
     */
    private static String[] splitSchemaId(final String id) {
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
    public static String dataTypeFromSchemaId(final String id)
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
    public static String domainFromSchemaId(final String id)
        throws ShimSchemaException {
        return splitSchemaId(id)[1];
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
                
        return new Schema(id, version.longValue(), concordia);
    }

    /**
     * Builds a Schema for a data type that only returns a single numeric value.
     *
     * @param id
     *        The schema ID.
     *
     * @param version
     *        The schema version.
     *
     * @param description
     *        The description.
     *
     * @return The Schema.
     */
    public static Schema buildSchemaForSingleValue(
        final String id,
        final Long version,
        final String description) 
        throws ShimSchemaException {
        String dataType = null;
        try {
            dataType = ShimUtil.dataTypeFromSchemaId(id);
        }
        catch(ShimSchemaException e) {
            throw new ShimSchemaException("Invalid schema id: " + id, e);
        }

        Map <String, Object> field = new HashMap<String, Object>();
        field.put("type", "number");
        field.put("name", dataType);
        field.put("doc", description);

        Map <String, Object> schema = new HashMap<String, Object>();
        schema.put("type", "object");
        schema.put("fields", Arrays.asList(field));

        Concordia concordia = null;
        try {
            concordia = new Concordia(objectToJsonNode(schema).toString());
        }
        catch(Exception e) {
            throw new ShimSchemaException(
                "Error creating Concordia object.", e);
        }

        return new Schema(id, version.longValue(), concordia);
    }

    /**
     * Fetches a URL.
     *
     * @param url
     *        The URL to fetch.
     *
     * @param headers
     *        Headers to send along with the request.
     *
     * @return An InputStream with the contents.
     */
    public static InputStream fetchUrl(
        final URL url, 
        final Map<String, String> headers,
        final String data) {
        InputStream inputStream = null;
        try {
            HttpURLConnection request = (HttpURLConnection)url.openConnection();

            if (headers != null) {
                for (String key : headers.keySet()) {
                    request.setRequestProperty(key, headers.get(key));
                }
            }

            if (data != null) {
                request.setRequestMethod("POST");
                request.setDoOutput(true);

                DataOutputStream outputStream = 
                    new DataOutputStream(request.getOutputStream());
                outputStream.writeBytes(data);
                outputStream.flush();
            }

            if (request.getResponseCode() != 200) {
                throw new OmhException(
                    "HTTP error code: " + request.getResponseCode());
            }

            inputStream = request.getInputStream();
        }
        catch(IOException e) {
            throw new OmhException("HTTP request error", e);
        }

        return inputStream;
    }

    /**
     * Fetches a URL.
     *
     * @param url
     *        The URL to fetch.
     *
     * @return An InputStream with the contents.
     */
    public static InputStream fetchUrl(final URL url) {
        return fetchUrl(url, null, null);
    }

    /**
     * Turn a generic Object into a JsonNode.
     *
     * @param object
     *        The object.
     *
     * @return The JsonNode.
     */
    public static JsonNode objectToJsonNode(final Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.valueToTree(object);
        }
        catch(Exception e) {
            throw new OmhException("JSON encoding error", e);
        }

        return jsonNode;
    }
}
