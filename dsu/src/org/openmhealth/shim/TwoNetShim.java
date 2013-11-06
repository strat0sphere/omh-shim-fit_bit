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

    /**
     * The base URL for all 2Net endpoints.
     */
    private static final String ENDPOINT_BASE_URL =
        "https://twonetcom.qualcomm.com/kernel/partner/";

    private final String authorizationHeader;

    /**
     * Class to represent how to extract a given type of data point from the
     * 2Net API.
     */
    private static class DataType {
        // Valid measure types.
        private final static String BLOOD = "blood";
        private final static String BODY = "body";
        private final static String BREATH = "breath";
        private final static String ENVIRONMENT = "environment";

        private final String deviceKey;
        private final String measureType;
        private final String measureName;
        private final String description;

        /**
         * @param deviceKey
         *        The extras key used to store the device's key.
         *
         * @param measureType
         *        The type of the measure.
         *
         * @param measureName
         *        The name of the measure.
         *
         * @param description
         *        The textual description of the data type.
         */
        public DataType(
            String deviceKey, 
            String measureType, 
            String measureName,
            String description) {
            if (deviceKey == null) {
                throw new OmhException("The deviceKey is null.");
            }
            if (measureType == null) {
                throw new OmhException("The measureType is null.");
            }
            if (measureName == null) {
                throw new OmhException("The measureName is null.");
            }
            if (description == null) {
                throw new OmhException("The description is null.");
            }

            this.deviceKey = deviceKey;
            this.measureType = measureType;
            this.measureName = measureName;
            this.description = description;
        }

        public String getDeviceKey() { return deviceKey; }
        public String getMeasureType() { return measureType; }
        public String getMeasureName() { return measureName; }
        public String getDescription() { return description; }
    }

    /**
     * Maps schema IDs to DataType objects.
     */
    private static Map<String, DataType> dataTypeMap = 
        new HashMap<String, DataType>();
    static {
        // Entra Glucometer
        dataTypeMap.put(
            "glucose_mg_per_dl",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_ENTRA_GLUCOMETER,
                DataType.BLOOD,
                "glucose",
                "Glucose level in mg/dL"));
        dataTypeMap.put(
            "temperature_f",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_ENTRA_GLUCOMETER,
                DataType.ENVIRONMENT,
                "temperature",
                "Environment ambient temperature in Fahrenheit"));

        // Nonin PulseOximeter
        dataTypeMap.put(
            "nonin_pulse_bpm",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_NONIN_PULSEOXIMETER,
                DataType.BLOOD,
                "pulse",
                "Pulse rate in beats per minute"));
        dataTypeMap.put(
            "spo2_percent",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_NONIN_PULSEOXIMETER,
                DataType.BLOOD,
                "spo2",
                "Blood oxygen level percentage"));

        // A&D Weight Scale
        dataTypeMap.put(
            "weight_lbs",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_AD_WEIGHT_SCALE,
                DataType.BODY,
                "weight",
                "Weight in pounds"));

        // A&D Blood Pressure Monitor
        dataTypeMap.put(
            "ad_pulse_bpm",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_AD_BLOOD_PRESSURE,
                DataType.BLOOD,
                "pulse",
                "Pulse rate in beats per minute"));
        dataTypeMap.put(
            "systolic_mmhg",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_AD_BLOOD_PRESSURE,
                DataType.BLOOD,
                "systolic",
                "Systolic pressure in mmHg"));
        dataTypeMap.put(
            "diastolic_mmhg",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_AD_BLOOD_PRESSURE,
                DataType.BLOOD,
                "diastolic",
                "Diastolic pressure in mmHg"));
        dataTypeMap.put(
            "map_mmhg",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_AD_BLOOD_PRESSURE,
                DataType.BLOOD,
                "map",
                "Mean Arterial Pressure in mmHg"));

        // Asthmapolis Spiroscout
        dataTypeMap.put(
            "inhale_count",
            new DataType(
                TwoNetShimAuthorization.KEY_EXTRAS_ASTHMAPOLIS_SPIROSCOUT,
                DataType.BREATH,
                "inhale",
                "Inhale count"));
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
        // We only have a version 1 for now, so return null for anything but 1.
        if (!version.equals(1L)) {
            return null;
        }

        DataType dataType = getDataType(id);

        return ShimUtil.buildSchemaForSingleValue(
            id, 
            version,
            dataType.getDescription());
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

        DataType dataType = getDataType(schemaId);

        // Build the request.
        Map<String, Object> request = new HashMap<String, Object>();

        String userGuid = UUID.randomUUID().toString();
        request.put(
            "guid", token.getExtra(TwoNetShimAuthorization.KEY_EXTRAS_USER));
        request.put("trackGuid", token.getExtra(dataType.getDeviceKey()));

        Map<String, Object> filter = new HashMap<String, Object>();

        // Apply date filters.
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

        // Execute the request.
        JsonNode response = fetchEndpoint("user/track/filtered", outerObject);

        // Build the return data from the response JSON.
        List<Data> outputData = new ArrayList<Data>();
        try {
            JsonNode measureArray =
                response
                    .get("trackResponse")
                    .get("measures")
                    .get("measure");

            // Iterate through the returned measures and create a Data for each
            // one.
            for (int i = numToSkip.intValue(); i < measureArray.size(); ++i) {
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

                // Stop if we have enough data points.
                if (outputData.size() >= numToReturn) {
                    break;
                }
            }
        }
        catch(Exception e) {
            throw new ShimDataException("Error fetching device data", e);
        }

        return outputData;
    }

    /**
     * Register a new user using the 2Net API.
     *
     * @return The guid of the user.
     */
    public String registerUser() {
        // Build the request.
        Map<String, Object> request = new HashMap<String, Object>();

        String userGuid = UUID.randomUUID().toString();
        request.put("guid", userGuid);

        Map<String, Object> outerObject = new HashMap<String, Object>();
        outerObject.put("registerRequest", request);

        // Execute the request.
        fetchEndpoint("register", outerObject);

        return userGuid;
    }

    /**
     * Register a device using the 2Net API.
     *
     * @param userGuid
     *        The guid of the user to register the device to.
     *
     * @param make
     *        The make of the device.
     *
     * @param model
     *        The model of the device.
     *
     * @param serialNumber
     *        The serial number of the device.
     *
     * @return The guid of the registered device.
     */
    public String registerDevice(
        final String userGuid,
        final String make,
        final String model,
        final String serialNumber) {
        // Build the request.
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

        // Execute the reqeust.
        JsonNode response = fetchEndpoint("user/track/register", outerObject);

        // Extract the returned guid.
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

    /**
     * Fetches a 2Net endpoint and parses the returned JSON.
     *
     * @param path
     *        Endpoint path.
     *
     * @param data
     *        Data to pass with request. If data is present, request is assumed
     *        to be a POST, otherwise GET.
     *
     * @return The parsed JSON response.
     */
    public JsonNode fetchEndpoint(
        final String path, 
        final Map<String, Object> data) {
        // Build the headers.
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", authorizationHeader);
        headers.put("Accept", "application/json");
        if (data != null) {
            headers.put("Content-Type", "application/json");
        }

        // Build the URL.
        URL endpointUrl = null;
        try {
            endpointUrl = new URL(ENDPOINT_BASE_URL + path);
        }
        catch(MalformedURLException e) {
            throw new OmhException("Error constructing URL", e);
        }

        // Execute the request.
        InputStream responseStream =
            ShimUtil.fetchUrl(
                endpointUrl, headers, 
                ShimUtil.objectToJsonNode(data).toString());

        // Parse the returned JSON.
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

    /**
     * Builds a property object used in 2Net API requests.
     *
     * @param name
     *        Property name
     *
     * @param value
     *        Property value
     *
     * @return Property object.
     */
    private Map<String, Object> buildRequestProperty(
        final String name, 
        final String value) {
        Map<String, Object> property = new HashMap<String, Object>();
        property.put("name", name);
        property.put("value", value);
        return property;
    }

    /**
     * Look up the DataType associated with the given schema ID.
     *
     * @param schemaId
     *        The schema ID.
     *
     * @return The associated DataType.
     */
    private DataType getDataType(final String schemaId) {
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

        return dataType;
    }
}
