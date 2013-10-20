package org.openmhealth.shim;

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import name.jenkins.paul.john.concordia.schema.ObjectSchema;
import name.jenkins.paul.john.concordia.validator.ValidationController;

import org.joda.time.DateTime;
import org.openmhealth.reference.domain.ColumnList;
import org.openmhealth.reference.domain.Data;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.domain.MetaData;
import org.openmhealth.reference.domain.Schema;
import org.openmhealth.shim.exception.ShimDataException;
import org.openmhealth.shim.exception.ShimSchemaException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FitbitShim implements Shim {
    public String getDomain() {
        return "fitbit";
    }

    public AuthorizationMethod getAuthorizationMethod() {
        return AuthorizationMethod.OAUTH_1;
    }

    public URL getRequestTokenUrl() {
        return buildURL("http://api.fitbit.com/oauth/request_token");
    }


	public URL getAuthorizeUrl() {
        return buildURL("http://www.fitbit.com/oauth/authorize");
    }

	public URL getTokenUrl() {
        return buildURL("http://api.fitbit.com/oauth/access_token");
    }

	public String getClientId() {
        return "6fa6c4f6e957429bb32bea2e4f5b59f4";
    }

	public String getClientSecret() {
        return "cae324dc35044c71ada7b4e43c1d1fb1";
    }

	public List<String> getSchemaIds() {
        return Arrays.asList("omh:fitbit:steps");
    }

	public List<Long> getSchemaVersions(
		final String id)
		throws ShimSchemaException {
        if (id == null) {
            throw new ShimSchemaException("Given ID is null.");
        }

        List<Long> versionList = new ArrayList<Long>();
        versionList.add(1L);
        return versionList;
    }

	public Schema getSchema(
		final String id,
		final Long version)
		throws ShimSchemaException {
        // Load and parse the Fitbit schema from the schema file.
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream schemaStream =
            getClass().getClassLoader().getResourceAsStream(
                "/schema/FitbitSchema.json");

        JsonNode schemaNode = null;
        try {
            schemaNode = objectMapper.readTree(schemaStream);
        }
        catch(IOException e) {
            throw new ShimSchemaException("Error reading schema.", e);
        }
                
        return new Schema(
            "omh:fitbit:steps", 1, schemaNode,
            ValidationController.BASIC_CONTROLLER);
    }

	public List<Data> getData(
		final String schemaId,
		final Long version,
		final ExternalAuthorizationToken token,
		final DateTime startDate,
		final DateTime endDate,
		final ColumnList columnList,
		final Long numToSkip,
		final Long numToReturn)
		throws ShimDataException {
        Schema schema = null;
        try {
            schema = getSchema("foo", 1L);
        }
        catch(ShimSchemaException e) {
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode dataPoint = null;
        try {
            dataPoint = objectMapper.readTree("{\"steps\": 3}");
        }
        catch(IOException e) {
            throw new ShimDataException("json error", e);
        }

        return Arrays.asList(
            new Data(
                "Test.User", schema, 
                new MetaData(null, DateTime.now()),
                dataPoint),
            new Data(
                "Test.User", schema, 
                new MetaData("metafoo", DateTime.now()),
                dataPoint));
    }

    private static URL buildURL(String urlStr) {
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
