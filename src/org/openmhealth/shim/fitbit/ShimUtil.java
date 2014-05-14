package org.openmhealth.shim.fitbit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import name.jenkins.paul.john.concordia.Concordia;

import org.openmhealth.reference.domain.Schema;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.exception.ShimSchemaException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ShimUtil {
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
            dataType = Schema.dataTypeFromSchemaId(id);
        }
        catch(ShimSchemaException e) {
            throw new ShimSchemaException("Invalid schema id: " + id, e);
        }

        Map <String, Object> field = new HashMap<String, Object>();
        field.put("type", "number");
        field.put("name", dataType);
        if (description != null) {
            field.put("doc", description);
        }

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
