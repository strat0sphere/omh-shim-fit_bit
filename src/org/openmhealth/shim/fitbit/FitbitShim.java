package org.openmhealth.shim.fitbit;

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
import java.util.Locale;
import java.util.Map;

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
import org.openmhealth.shim.authorization.oauth1.OAuth1Authorization;
import org.openmhealth.shim.exception.ShimDataException;
import org.openmhealth.shim.exception.ShimSchemaException;
import org.openmhealth.shim.Shim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.client.FitbitAPIEntityCache;
import com.fitbit.api.client.FitbitApiClientAgent;
import com.fitbit.api.client.FitbitApiCredentialsCache;
import com.fitbit.api.client.FitbitApiCredentialsCacheMapImpl;
import com.fitbit.api.client.FitbitApiEntityCacheMapImpl;
import com.fitbit.api.client.FitbitApiSubscriptionStorage;
import com.fitbit.api.client.FitbitApiSubscriptionStorageInMemoryImpl;
import com.fitbit.api.client.LocalUserDetail;
import com.fitbit.api.client.service.FitbitAPIClientService;
import com.fitbit.api.common.model.activities.ActivitiesSummary;
import com.fitbit.api.common.model.activities.ActivityDistance;
import com.fitbit.api.common.model.sleep.SleepSummary;
import com.fitbit.api.model.APIResourceCredentials;
import com.fitbit.api.model.FitbitUser;

public class FitbitShim implements Shim {
    private static final String DOMAIN = "fitbit";

    /**
     * The prefix for all schemas used in this shim.
     */
    private static final String SCHEMA_PREFIX = "omh:" + DOMAIN + ":";

    /**
     * Used internally by the Fitbit lib.
     */
    private FitbitAPIEntityCache entityCache =
        new FitbitApiEntityCacheMapImpl();
    private FitbitApiCredentialsCache credentialsCache = 
        new FitbitApiCredentialsCacheMapImpl();
    private FitbitApiSubscriptionStorage subscriptionStore = 
        new FitbitApiSubscriptionStorageInMemoryImpl();

    /**
     * Fitbit API client object.
     */
    private FitbitAPIClientService<FitbitApiClientAgent> apiClientService;

    /**
     * Interface for the data fetchers used in the dataFetcherMap below. One
     * DataFetcher will be defined for each supported API endpoint.
     */
    private interface DataFetcher {
        /**
         * Fetches the value of a single field for the given date.
         *
         * @param client
         *        The Fitbit API client.
         *
         * @param localUserDetail
         *        The Fitbit user to fetch the data for.
         *
         * @param date
         *        The date of the data.
         *
         * @param field
         *        The field to fetch.
         *
         * @return The value of the field.
         */
        public Object dataForDay(
            FitbitAPIClientService<FitbitApiClientAgent> client,
            LocalUserDetail localUserDetail, DateTime date,
            DataType.Field field);
    }

    private static final DataFetcher activitiesFetcher =
        new DataFetcher() {
            public Object dataForDay(
                FitbitAPIClientService<FitbitApiClientAgent> client,
                LocalUserDetail localUserDetail, DateTime date,
                DataType.Field field) {
                return activityForDay(client, localUserDetail, date, field);
            }
        };
    
    private static final DataFetcher sleepFetcher =
        new DataFetcher() {
            public Object dataForDay(
                FitbitAPIClientService<FitbitApiClientAgent> client,
                LocalUserDetail localUserDetail, DateTime date,
                DataType.Field field) {
                return sleepForDay(client, localUserDetail, date, field);
            }
        };

    /**
     * Class to represent how to extract a given type of data point from the
     * Fitbit API.
     */
    private static class DataType {
        public enum Field {
            // Activities
            CALORIES,
            STEPS,
            DISTANCE,
            FLOORS,
            ELEVATION,
            SEDENTARY_MINUTES,
            LIGHTLY_ACTIVE_MINUTES,
            FAIRLY_ACTIVE_MINUTES,
            VERY_ACTIVE_MINUTES,
            ACTIVITY_CALORIES,

            // Sleep
            TIME_ASLEEP,
            TIME_IN_BED,
        }

        private final DataFetcher fetcher;
        private final Field field;

        /**
         * @param fetcher
         *        The DataFetcher used to retrieve the data.
         *
         * @param field
         *        The individual field to extract from the data.
         */
        public DataType(
            DataFetcher fetcher, 
            Field field) {
            if (fetcher == null) {
                throw new OmhException("The fetcher is null.");
            }
            if (field == null) {
                throw new OmhException("The field is null.");
            }

            this.fetcher = fetcher;
            this.field = field;
        }

        public DataFetcher getFetcher() { return fetcher; }
        public Field getField() { return field; }
    }

    /**
     * Maps schema IDs to DataType objects.
     */
    private static Map<String, DataType> dataTypeMap = 
        new HashMap<String, DataType>();
    static {
        // Activities
        dataTypeMap.put(
            "calories", 
            new DataType(activitiesFetcher, DataType.Field.CALORIES));
        dataTypeMap.put(
            "steps", 
            new DataType(activitiesFetcher, DataType.Field.STEPS));
        dataTypeMap.put(
            "distance_mi", 
            new DataType(activitiesFetcher, DataType.Field.DISTANCE));
        dataTypeMap.put(
            "floors", 
            new DataType(activitiesFetcher, DataType.Field.FLOORS));
        dataTypeMap.put(
            "elevation_ft", 
            new DataType(activitiesFetcher, DataType.Field.ELEVATION));
        dataTypeMap.put(
            "sedentary_minutes",
            new DataType(activitiesFetcher, DataType.Field.SEDENTARY_MINUTES));
        dataTypeMap.put(
            "lightly_active_minutes", 
            new DataType(
                activitiesFetcher, DataType.Field.LIGHTLY_ACTIVE_MINUTES));
        dataTypeMap.put(
            "fairly_active_minutes", 
            new DataType(
                activitiesFetcher, DataType.Field.FAIRLY_ACTIVE_MINUTES));
        dataTypeMap.put(
            "very_active_minutes", 
            new DataType(activitiesFetcher, DataType.Field.VERY_ACTIVE_MINUTES));
        dataTypeMap.put(
            "activity_calories", 
            new DataType(activitiesFetcher, DataType.Field.ACTIVITY_CALORIES));

        // Sleep
        dataTypeMap.put(
            "time_asleep_minutes", 
            new DataType(sleepFetcher, DataType.Field.TIME_ASLEEP));
        dataTypeMap.put(
            "time_in_bed_minutes", 
            new DataType(sleepFetcher, DataType.Field.TIME_IN_BED));
    }

    public FitbitShim() {
        String clientId = System.getProperty(DOMAIN + ".clientId");
        String clientSecret = System.getProperty(DOMAIN + ".clientSecret");
        if (clientId == null || clientSecret == null) {
            throw new OmhException(
                DOMAIN + ".clientId and " + DOMAIN + ".clientSecret"
                + " must be set in the properties file.");
        }

        apiClientService = 
            new FitbitAPIClientService<FitbitApiClientAgent>(
                new FitbitApiClientAgent(
                    "api.fitbit.com", "http://www.fitbit.com", 
                    credentialsCache),
                clientId,
                clientSecret,
                credentialsCache,
                entityCache,
                subscriptionStore);
        apiClientService.getClient().setLocale(Locale.US);
    }

    public FitbitAPIClientService<FitbitApiClientAgent> getApiClientService() {
        return apiClientService;
    }

    public FitbitApiCredentialsCache getCredentialsCache() {
        return credentialsCache;
    }

    public String getDomain() {
        return DOMAIN;
    }

	public ShimAuthorization getAuthorizationImplementation() {
        return new FitbitShimAuthorization();
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

        return ShimUtil.buildSchemaForSingleValue(id, version, null);
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

        LocalUserDetail localUserDetail =
            new LocalUserDetail(token.getUsername());

        // Store the user's access token in the cache.
        APIResourceCredentials credentials =
            new APIResourceCredentials(token.getUsername(), null, null);
        credentials.setAccessToken(token.getAccessToken());
        credentials.setAccessTokenSecret(
            token.<String>getExtra(OAuth1Authorization.KEY_EXTRAS_SECRET));
        credentialsCache.saveResourceCredentials(localUserDetail, credentials);

        // Extract the data type and find the associated DataType.
        String dataTypeString = null;
        try {
            dataTypeString = ShimUtil.dataTypeFromSchemaId(schemaId);
        }
        catch(ShimSchemaException e) {
            throw new ShimDataException("Invalid schema id: " + schemaId, e);
        }
        DataType dataType = getDataType(schemaId);

        // Fetch the data.
        List<Data> outputData = new ArrayList<Data>();
        DateTime dateToFetch = null;
        if (endDate == null) {
            dateToFetch = DateTime.now();
        } else {
            dateToFetch = endDate;
        }
        dateToFetch = 
            dateToFetch.withTime(0, 0, 0, 0).minusDays(numToSkip.intValue());
        for(; outputData.size() < numToReturn 
              && (startDate == null || dateToFetch.compareTo(startDate) > 0)
            ; dateToFetch = dateToFetch.minusDays(1)) {
            Object value = 
                dataType.getFetcher().dataForDay(
                    apiClientService, localUserDetail, dateToFetch, 
                    dataType.getField());
            Map<String, Object> outputDatum = new HashMap<String, Object>();
            outputDatum.put(dataTypeString, value);

            outputData.add(
                new Data(
                    token.getUsername(), schemaId, version,
                    new MetaData(null, dateToFetch),
                    ShimUtil.objectToJsonNode(outputDatum)));
        }

        return outputData;
    }

    // See the DataFetcher interface.
    private static Object activityForDay(
        FitbitAPIClientService<FitbitApiClientAgent> client,
        LocalUserDetail localUserDetail, DateTime date,
        DataType.Field field) {
        // Fetch the data.
        ActivitiesSummary summary = null;
        try {
            summary = client.getClient().getActivities(
                localUserDetail, FitbitUser.CURRENT_AUTHORIZED_USER, 
                date.toLocalDate()).getSummary();
        }
        catch(FitbitAPIException e) {
            throw new ShimDataException("Fitbit API error", e);
        }

        // Return the specific field.
        switch(field) {
            case CALORIES:
                return summary.getCaloriesOut();

            case STEPS:
                return summary.getSteps();

            case DISTANCE:
                double distance = 0;
                for(ActivityDistance d : summary.getDistances()) {
                    if (d.getActivity().equals("total")) {
                        distance = d.getDistance();
                        break;
                    }
                }
                return distance;

            case FLOORS:
                return summary.getFloors();

            case ELEVATION:
                return summary.getElevation();

            case SEDENTARY_MINUTES:
                return summary.getSedentaryMinutes();

            case LIGHTLY_ACTIVE_MINUTES:
                return summary.getLightlyActiveMinutes();

            case FAIRLY_ACTIVE_MINUTES:
                return summary.getFairlyActiveMinutes();

            case VERY_ACTIVE_MINUTES:
                return summary.getVeryActiveMinutes();

            case ACTIVITY_CALORIES:
                return summary.getActivityCalories();

            default:
                throw new OmhException("Unknown activities Field");
        }
    }

    // See the DataFetcher interface.
    private static Object sleepForDay(
        FitbitAPIClientService<FitbitApiClientAgent> client,
        LocalUserDetail localUserDetail, DateTime date,
        DataType.Field field) {
        // Fetch the data.
        SleepSummary summary = null;
        try {
            summary = client.getClient().getSleep(
                localUserDetail, FitbitUser.CURRENT_AUTHORIZED_USER, 
                date.toLocalDate()).getSummary();
        }
        catch(FitbitAPIException e) {
            throw new ShimDataException("Fitbit API error", e);
        }

        // Return the specific field.
        switch(field) {
            case TIME_ASLEEP:
                return summary.getTotalMinutesAsleep();

            case TIME_IN_BED:
                return summary.getTotalTimeInBed();

            default:
                throw new OmhException("Unknown sleep Field");
        }
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
