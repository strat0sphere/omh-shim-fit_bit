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
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.authorization.ShimAuthorization;
import org.openmhealth.shim.authorization.oauth1.OAuth1Authorization;
import org.openmhealth.shim.exception.ShimDataException;
import org.openmhealth.shim.exception.ShimSchemaException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.UrlStringRequestAdapter;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpParameters;
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

	public ShimAuthorization getAuthorizationImplementation() {
        return new WithingsShimAuthorization();
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

        // Build request URL.
        StringBuilder dateParams = new StringBuilder();
        if (startDate != null) {
            dateParams.append("&startdate=" + startDate.getMillis() / 1000);
        }
        if (endDate != null) {
            dateParams.append("&enddate=" + endDate.getMillis() / 1000);
        }

        URL url = 
            buildSignedUrl(
                "http://wbsapi.withings.net/measure"
                + "?action=getmeas"
                + "&devtype=1"
                + dateParams.toString()
                + "&userid=" 
                + token.getExtra(WithingsShimAuthorization.KEY_EXTRAS_USERID),
                token,
                null);

        // Fetch and decode the JSON data.
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonData = null;
        try {
            jsonData = objectMapper.readTree(ShimUtil.fetchUrl(url));
        }
        catch(IOException e) {
            throw new ShimDataException("JSON decoding error", e);
        }

        // Make sure there's no error status.
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
        long numSkipped = 0;
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

                // Skip the required number before saving any.
                if (numSkipped < numToSkip.longValue()) {
                    ++numSkipped;
                    break;
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

            // Stop if we have enough data points.
            if (outputData.size() >= numToReturn) {
                break;
            }
        }

        return outputData;
    }

    /**
     * Signs a URL.
     *
     * @param unsignedUrl
     *        The URL to be signed.
     *
     * @param token
     *        The token. Can be null.
     *
     * @param secret
     *        The secret. Can be null.
     *
     * @param parameters
     *        Additional parameters for the URL.
     *
     * @return A signed URL.
     */
    public URL buildSignedUrl(
        String unsignedUrl, 
        String token, 
        String secret, 
        Map<String, String> parameters) {
        // Build the oauth consumer used for signing requests.
        OAuthConsumer consumer = createOAuthConsumer();
        if (token != null && secret != null) {
            consumer.setTokenWithSecret(token, secret);
        }

        if (parameters != null) {
            HttpParameters additionalParameters = new HttpParameters();
            for (String key : parameters.keySet()) {
                additionalParameters.put(key, parameters.get(key));
            }
            consumer.setAdditionalParameters(additionalParameters);
        }

        URL url = null;
        try {
            UrlStringRequestAdapter adapter = 
                new UrlStringRequestAdapter(unsignedUrl);

            consumer.sign(adapter);

            url = new URL(adapter.getRequestUrl());
        }
        catch(MalformedURLException
              | OAuthExpectationFailedException
              | OAuthCommunicationException
              | OAuthMessageSignerException
              e) {
            throw new ShimDataException("Error signing URL", e);
        }

        return url;
    }

    /**
     * Signs a URL.
     *
     * @param unsignedUrl
     *        The URL to be signed.
     *
     * @param token
     *        The token.
     *
     * @param parameters
     *        Additional parameters for the URL.
     *
     * @return A signed URL.
     */
    public URL buildSignedUrl(
        String unsignedUrl, ExternalAuthorizationToken token,
        Map<String, String> parameters) {
        return buildSignedUrl(
            unsignedUrl,
            token.getAccessToken(), 
            token.<String>getExtra(OAuth1Authorization.KEY_EXTRAS_SECRET),
            parameters);
    }

    /**
     * Creates an oauth consumer used for signing request URLs.
     *
     * @return The OAuthConsumer.
     */
    private OAuthConsumer createOAuthConsumer() {
        String clientId = System.getProperty(DOMAIN + ".clientId");
        String clientSecret = System.getProperty(DOMAIN + ".clientSecret");
        if (clientId == null || clientSecret == null) {
            throw new OmhException(
                DOMAIN + ".clientId and " + DOMAIN + ".clientSecret"
                + " must be set in the properties file.");
        }

        OAuthConsumer consumer =
            new DefaultOAuthConsumer(clientId, clientSecret);
        consumer.setSigningStrategy(new QueryStringSigningStrategy());
        return consumer;
    }
}
