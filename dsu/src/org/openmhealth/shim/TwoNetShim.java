package org.openmhealth.shim;

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import name.jenkins.paul.john.concordia.Concordia;
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
import org.openmhealth.shim.exception.ShimDataException;
import org.openmhealth.shim.exception.ShimSchemaException;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TwoNetShim implements Shim {
    private static final String DOMAIN = "twonet";

    /**
     * The prefix for all schemas used in this shim.
     */
    private static final String SCHEMA_PREFIX = "omh:" + DOMAIN + ":";

    private final String authorizationHeader;

    private static class DataType {
        private final static String BLOOD = "blood";
        private final static String ENVIRONMENT = "environment";

        private final String deviceKey;
        private final String measureType;
        private final String measureName;

        public DataType(
            String deviceKey, 
            String measureType, 
            String measureName) {
            if (deviceKey == null) {
                throw new OmhException("The deviceKey is null.");
            }
            if (measureType == null) {
                throw new OmhException("The measureType is null.");
            }
            if (measureName == null) {
                throw new OmhException("The measureName is null.");
            }

            this.deviceKey = deviceKey;
            this.measureType = measureType;
            this.measureName = measureName;
        }

        public String getDeviceKey() { return deviceKey; }
        public String getMeasureType() { return measureType; }
        public String getMeasureName() { return measureName; }
    }

    private static Map<String, DataType> dataTypeMap = 
        new HashMap<String, DataType>();
    static {
        dataTypeMap.put(
            "glucose_mg_per_dl",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_ENTRA_GLUCOMETER,
                DataType.BLOOD,
                "glucose"));

        dataTypeMap.put(
            "temperature_f",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_ENTRA_GLUCOMETER,
                DataType.ENVIRONMENT,
                "temperature"));
    }

    public TwoNetShim() {
        authorizationHeader =
            "Basic " +
            Base64.encodeBase64String(
                (ShimUtil.getShimProperty(DOMAIN, "key")
                 + ":"
                 + ShimUtil.getShimProperty(DOMAIN, "secret")).getBytes());
    }

    public String getDomain() {
        return DOMAIN;
    }

	public ShimAuthorization getAuthorizationImplementation() {
        return new TwoNetShimAuthorization();
    }

	public List<String> getSchemaIds() {
        List<String> schemaIds = new ArrayList<String>();
        for (String key : dataTypeMap.keySet()) {
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

        // Extract the data type and find the associated DataType.
        String dataTypeString = null;
        try {
            dataTypeString = ShimUtil.dataTypeFromSchemaId(schemaId);
        }
        catch(ShimSchemaException e) {
            throw new ShimDataException("Invalid schema id: " + schemaId, e);
        }
        DataType dataType = dataTypeMap.get(dataTypeString);
        if (dataType == null) {
            throw new ShimDataException("Unknown schema id: " + schemaId);
        }

        // Fetch the data.
        Map<String, Object> request = new HashMap<String, Object>();

        String userGuid = UUID.randomUUID().toString();
        request.put(
            "guid", token.getExtra(TwoNetShimAuthorization.KEY_EXTRAS_USER));
        request.put("trackGuid", token.getExtra(dataType.getDeviceKey()));

        Map<String, Object> filter = new HashMap<String, Object>();

        if (startDate != null) {
            filter.put("startDate", startDate.getMillis() / 1000);
        }
        if (endDate != null) {
            filter.put("endDate", endDate.getMillis() / 1000);
        }

        if (filter.size() > 0) {
            request.put("filter", filter);
        }

        Map<String, Object> outerObject = new HashMap<String, Object>();
        outerObject.put("trackRequest", request);

        JsonNode response = fetchEndpoint("user/track/filtered", outerObject);

        List<Data> outputData = new ArrayList<Data>();
        try {
            JsonNode measureArray =
                response
                    .get("trackResponse")
                    .get("measures")
                    .get("measure");

            for (int i = 0; i < measureArray.size(); ++i) {
                JsonNode measure = measureArray.get(i);

                DateTime dateTime = 
                    new DateTime(measure.get("time").asLong() * 1000L);

                JsonNode value =
                    measure
                        .get(dataType.getMeasureType())
                        .get(dataType.getMeasureName());

                Map<String, Object> outputDatum = new HashMap<String, Object>();
                outputDatum.put(dataTypeString, value.asDouble());

                outputData.add(
                    new Data(
                        token.getUsername(), schemaId, version.longValue(),
                        new MetaData(null, dateTime),
                        ShimUtil.objectToJsonNode(outputDatum)));
            }
        }
        catch(Exception e) {
            throw new ShimDataException("Error fetching device data", e);
        }

        return outputData;
    }

    public String registerUser() {
        Map<String, Object> request = new HashMap<String, Object>();

        String userGuid = UUID.randomUUID().toString();
        request.put("guid", userGuid);

        Map<String, Object> outerObject = new HashMap<String, Object>();
        outerObject.put("registerRequest", request);

        fetchEndpoint("register", outerObject);

        return userGuid;
    }

    public String registerDevice(
        String userGuid,
        String make,
        String model,
        String serialNumber) {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("guid", userGuid);
        request.put("type", "2net");
        request.put("registerType", "properties");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(
            "property",
            Arrays.asList(
                buildRequestProperty("make", make),
                buildRequestProperty("model", model),
                buildRequestProperty("serialNumber", serialNumber)));
        request.put("properties", properties);

        Map<String, Object> outerObject = new HashMap<String, Object>();
        outerObject.put("trackRegistrationRequest", request);

        JsonNode response = fetchEndpoint("user/track/register", outerObject);

        String deviceGuid = null;
        try {
            deviceGuid = 
                response
                    .get("trackRegistrationResponse")
                    .get("trackDetail")
                    .get("guid")
                    .asText();
        }
        catch(Exception e) {
            throw new OmhException("Device registration error", e);
        }

        return deviceGuid;
    }

    public JsonNode fetchEndpoint(String path, Map<String, Object> data) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", authorizationHeader);
        headers.put("Accept", "application/json");
        if (data != null) {
            headers.put("Content-Type", "application/json");
        }

        URL endpointUrl = null;
        try {
            endpointUrl = 
                new URL(
                    "https://twonetcom.qualcomm.com/kernel/partner/"
                    + path);
        }
        catch(MalformedURLException e) {
            throw new OmhException("Error constructing URL", e);
        }

        InputStream responseStream =
            ShimUtil.fetchUrl(
                endpointUrl, headers, 
                ShimUtil.objectToJsonNode(data).toString());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseJson = null;
        try {
            responseJson = objectMapper.readTree(responseStream);
        }
        catch(IOException e) {
            throw new OmhException("JSON decoding error", e);
        }

        return responseJson;
    }

    private Map<String, Object> buildRequestProperty(
        String name, String value) {
        Map<String, Object> property = new HashMap<String, Object>();
        property.put("name", name);
        property.put("value", value);
        return property;
    }
}
