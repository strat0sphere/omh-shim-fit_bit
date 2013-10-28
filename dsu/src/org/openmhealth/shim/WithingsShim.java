package org.openmhealth.shim;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.UrlStringRequestAdapter;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.signature.QueryStringSigningStrategy;

public class WithingsShim implements Shim {
    private static final String DOMAIN = "withings";

    /**
     * The prefix for all schemas used in this shim.
     */
    private static final String SCHEMA_PREFIX = "omh:" + DOMAIN + ":";

    /**
     * Maps schemaID to Withings measure type for each supported domain.
     * Measure type is a numeric value defined in the Withings API docs found:
     * http://www.withings.com/en/api
     */
    private static Map<String, Integer> measureTypeMap = 
        new HashMap<String, Integer>();
    static {
        measureTypeMap.put("weight_kg", 1);
        measureTypeMap.put("height_m", 4);
        measureTypeMap.put("fat_free_mass_kg", 5);
        measureTypeMap.put("fat_ratio_percent", 6);
        measureTypeMap.put("fat_mass_kg", 8);
        measureTypeMap.put("diastolic_blood_pressure_mmhg", 9);
        measureTypeMap.put("systolic_blood_pressure_mmhg", 10);
        measureTypeMap.put("heart_pulse_bpm", 11);
    }

    public String getDomain() {
        return DOMAIN;
    }

    public AuthorizationMethod getAuthorizationMethod() {
        return AuthorizationMethod.OAUTH_1;
    }

    public URL getRequestTokenUrl() {
        return null;
    }

	public URL getAuthorizeUrl() {
        return null;
    }

	public URL getTokenUrl() {
        return null;
    }

	public String getClientId() {
        return "bfb8c6b3bffd8b83b39e67dfe40f81c8289b8d0bbfb97b27953925d6f3bc";
    }

	public String getClientSecret() {
        return "d9182878bc9999158cd748fc2fe12d81ffcce9c9f77093972e93c0f";
    }

	public List<String> getSchemaIds() {
        List<String> schemaIds = new ArrayList<String>();
        for (String key : measureTypeMap.keySet()) {
            schemaIds.add(SCHEMA_PREFIX + key);
        }
        return schemaIds;
    }

	public List<Long> getSchemaVersions(
		final String id)
		throws ShimSchemaException {
        // This shim doesn't (yet) have more than one version of a schema for
        // anything, so we just hardcode the return here.
        List<Long> versionList = new ArrayList<Long>();
        versionList.add(1L);
        return versionList;
    }

	public Schema getSchema(
		final String id,
		final Long version)
		throws ShimSchemaException {
        return ShimUtil.getSchema(id, version);
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
        // We only have a version 1 for now, so return null early for anything
        // but 1.
        if (!version.equals(1L)) {
            return null;
        }

        // Build the oauth consumer used for signing requests.
        OAuthConsumer consumer = 
            new DefaultOAuthConsumer(getClientId(), getClientSecret());
        consumer.setSigningStrategy(new QueryStringSigningStrategy());
        consumer.setTokenWithSecret(
            token.getAccessToken(), token.getAccessTokenSecret());

        // Build and sign the request URL.
        URL url = null;
        try {
            UrlStringRequestAdapter adapter = 
                new UrlStringRequestAdapter(
                    "http://wbsapi.withings.net/measure"
                    + "?action=getmeas"
                    + "&devtype=1"
                    + "&userid=2406179");

            consumer.sign(adapter);

            url = new URL(adapter.getRequestUrl());
        }
        catch(MalformedURLException
              | OAuthExpectationFailedException
              | OAuthCommunicationException
              | OAuthMessageSignerException
              e) {
            throw new ShimDataException("Error constructing URL", e);
        }

        // Fetch and decode the JSON data.
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonData = null;
        try {
            HttpURLConnection request = (HttpURLConnection)url.openConnection();
            request.connect();

            if (request.getResponseCode() != 200) {
                throw new ShimDataException(
                    "Withings HTTP error code: " 
                    + request.getResponseCode());
            }

            jsonData = objectMapper.readTree(request.getInputStream());
        }
        catch(IOException e) {
            throw new ShimDataException("Withings API request error", e);
        }

        JsonNode requestStatus = jsonData.get("status");
        if (requestStatus == null) {
            throw new ShimDataException("No request status in returned data");
        } else if (requestStatus.asInt() != 0) {
            throw new ShimDataException(
                "Withings API request error: " + requestStatus.asInt());
        }

        // Extract the data type and find the associated measure type.
        String dataType = null;
        try {
            dataType = ShimUtil.dataTypeFromSchemaId(schemaId);
        }
        catch(ShimSchemaException e) {
            throw new ShimDataException("Invalid schema id: " + schemaId, e);
        }
        Integer measureType = measureTypeMap.get(dataType);
        if (measureType == null) {
            throw new ShimDataException("Unknown schema id: " + schemaId);
        }

        // Extract the measurement groups.
        JsonNode body = jsonData.get("body");
        if (body == null) {
            throw new ShimDataException("No body found");
        }
        JsonNode measurementGroups = body.get("measuregrps");
        if (measurementGroups == null) {
            throw new ShimDataException("No measurement groups found");
        }

        // Filter the JSON for the measure type and build the output data.
        List<Data> outputData = new ArrayList<Data>();
        for (int i = 0; i < measurementGroups.size(); ++i) {
            JsonNode measurementGroup = measurementGroups.get(i);
            JsonNode dateJson = measurementGroup.get("date");
            if (dateJson == null) {
                continue;
            }
            DateTime groupDateTime = new DateTime(dateJson.asLong() * 1000L);

            // Look through all the measures for the right measure type.
            JsonNode measures = measurementGroup.get("measures");
            for (int j = 0; j < measures.size(); ++j) {
                JsonNode measure = measures.get(j);

                JsonNode typeJson = measure.get("type");
                JsonNode valueJson = measure.get("value");
                if (typeJson == null || valueJson == null
                    || typeJson.asInt() != measureType.intValue()) {
                    continue;
                }

                double value = valueJson.asDouble();

                // Scale by the given unit if present.
                JsonNode unitJson = measure.get("unit");
                if (unitJson != null) {
                    value *= Math.pow(10, unitJson.asDouble());
                }
                
                // Build and add the output datum.
                Map<String, Object> outputDatum = 
                    new HashMap<String, Object>();
                outputDatum.put(dataType, new Double(value));

                JsonNode outputJson = objectMapper.valueToTree(outputDatum);

                outputData.add(
                    new Data(token.getUsername(), schemaId, version.longValue(),
                        new MetaData(null, groupDateTime), outputJson));

                break;
            }
        }

        return outputData;
    }
}
