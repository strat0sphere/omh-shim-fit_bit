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
import com.fitbit.api.common.model.activities.Activities;
import com.fitbit.api.model.APIResourceCredentials;
import com.fitbit.api.model.FitbitUser;

public class FitbitShim implements Shim {
    private FitbitAPIEntityCache entityCache =
        new FitbitApiEntityCacheMapImpl();

    private FitbitApiCredentialsCache credentialsCache = 
        new FitbitApiCredentialsCacheMapImpl();

    private FitbitApiSubscriptionStorage subscriptionStore = 
        new FitbitApiSubscriptionStorageInMemoryImpl();

    private FitbitAPIClientService<FitbitApiClientAgent> apiClientService;

    public FitbitShim() {
         apiClientService = 
             new FitbitAPIClientService<FitbitApiClientAgent>(
                 new FitbitApiClientAgent(
                     "api.fitbit.com", "http://www.fitbit.com", 
                     credentialsCache),
                 getClientId(),
                 getClientSecret(),
                 credentialsCache,
                 entityCache,
                 subscriptionStore);
    }

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
        return Arrays.asList("omh:fitbit:activity");
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
            "omh:fitbit:activity", 1, schemaNode,
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
        LocalUserDetail localUserDetail =
            new LocalUserDetail(token.getUsername());

        // Store the user's access token in the cache.
        APIResourceCredentials credentials =
            new APIResourceCredentials(token.getUsername(), null, null);
        credentials.setAccessToken(token.getAccessToken());
        credentials.setAccessTokenSecret(token.getAccessTokenSecret());
        credentialsCache.saveResourceCredentials(localUserDetail, credentials);

        Activities activities = null;
        try {
            activities = apiClientService.getClient().getActivities(
                localUserDetail, FitbitUser.CURRENT_AUTHORIZED_USER, 
                startDate.toLocalDate());
        }
        catch(FitbitAPIException e) {
            throw new ShimDataException("Fitbit API error", e);
        }

        // Fetch the schema.
        Schema schema = null;
        try {
            schema = getSchema(schemaId, 1L);
        }
        catch(ShimSchemaException e) {
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode dataPoint = null;
        try {
            dataPoint = objectMapper.readTree(
                "{\"steps\": "
                + activities.getSummary().getSteps()
                + "}");
        }
        catch(IOException e) {
            throw new ShimDataException("json error", e);
        }

        return Arrays.asList(
            new Data(
                token.getUsername(), schema, 
                new MetaData(null, startDate),
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
