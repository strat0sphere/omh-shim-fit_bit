/*******************************************************************************
 * Copyright 2013 Open mHealth
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.openmhealth.reference.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.openmhealth.reference.data.DataSet;
import org.openmhealth.reference.data.ExternalAuthorizationTokenBin;
import org.openmhealth.reference.data.Registry;
import org.openmhealth.reference.domain.AuthenticationToken;
import org.openmhealth.reference.domain.AuthorizationToken;
import org.openmhealth.reference.domain.ColumnList;
import org.openmhealth.reference.domain.Data;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.domain.MultiValueResult;
import org.openmhealth.reference.domain.MultiValueResultAggregator;
import org.openmhealth.reference.exception.InvalidAuthenticationException;
import org.openmhealth.reference.exception.InvalidAuthorizationException;
import org.openmhealth.reference.exception.NoSuchSchemaException;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.reference.servlet.Version1;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.ShimRegistry;

/**
 * <p>
 * Retrieves data based on the given parameters.
 * </p>
 *
 * @author John Jenkins
 */
public class DataReadRequest extends ListRequest<Data> {
	/**
	 * The authentication token for the requesting user.
	 */
	private final AuthenticationToken authenticationToken;
	/**
	 * The authorization token for the requesting third-party.
	 */
	private final AuthorizationToken authorizationToken;
	/**
	 * The ID of the schema from which the data was generated.
	 */
	private final String schemaId;
	/**
	 * The version of the schema from which the data was generated.
	 */
	private final long version;
	/**
	 * The identifier for the user to which the data should belong.
	 */
	private final String owner;
	/**
	 * The earliest point from which data should be read.
	 */
	private final DateTime startDate;
	/**
	 * The latest point from which data should be read.
	 */
	private final DateTime endDate;
	/**
	 * The list of columns to select from the data.
	 */
	private final ColumnList columnList;

	/**
	 * Creates a request for data.
	 * 
	 * @param authenticationToken The requesting user's authentication token.
	 * 
	 * @param authorizationToken The third-party's authorization token.
	 * 
	 * @param schemaId The ID of the schema from which the data was generated.
	 * 
	 * @param version The version of the schema from which the data was
	 * 				  generated.
	 * 
	 * @param owner Defines whose data should be read.
	 * 
	 * @param startDate The earliest point from which data should be read.
	 * 
	 * @param endDate The latest point from which data should be read.
	 * 
	 * @param columnList The list of columns in the data to return.
	 * 
	 * @param numToSkip The number of data points to skip.
	 * 
	 * @param numToReturn The number of data points to return.
	 * 
	 * @throws OmhException A parameter was invalid.
	 */
	public DataReadRequest(
		final AuthenticationToken authenticationToken,
		final AuthorizationToken authorizationToken,
		final String schemaId,
		final long version,
		final String owner,
		final DateTime startDate,
		final DateTime endDate,
		final List<String> columnList,
		final Long numToSkip,
		final Long numToReturn)
		throws OmhException {
		
		super(numToSkip, numToReturn);
		
		if((authenticationToken == null) && (authorizationToken == null)) {
			throw
				new InvalidAuthenticationException(
					"No authentication information was provided.");
		}
		if(schemaId == null) {
			throw new OmhException("The schema ID is missing.");
		}

		this.authenticationToken = authenticationToken;
		this.authorizationToken = authorizationToken;
		this.schemaId = schemaId;
		this.version = version;
		this.owner = owner;
		this.startDate = startDate;
		this.endDate = endDate;
		this.columnList = new ColumnList(columnList);
	}

	/**
	 * Authenticates the user, authorizes the request if it was for data that
	 * belongs to a different user, and retrieves the applicable data.
	 */
	@Override
	public void service() throws OmhException {
		// First, short-circuit if this request has already been serviced.
		if(isServiced()) {
			return;
		}
		else {
			setServiced();
		}
		
		// Create a handle for the validated user-name of the user whose data
		// is desired.
		String username = owner;
		
		// Get the user-name associated with the authentication token, if one
		// was given.
		String authenticationUsername = null;
		if(authenticationToken != null) {
			authenticationUsername = authenticationToken.getUsername();
		}
		
		// Get the user-name associated with the authorization token, if one
		// was given.
		String authorizationUsername = null;
		if(authorizationToken != null) {
			authorizationUsername =
				authorizationToken
					.getAuthorizationCodeVerification()
					.getOwnerUsername();
		}
		
		// If the requester did not give a user-name use the authentication and
		// authorization tokens to infer one.
		if(username == null) {
			// First, check the authorization token, which should only ever be
			// given when information about a user other than the requester is
			// desired.
			if(authorizationUsername != null) {
				username = authorizationUsername;
			}
			// If that wasn't given, fall back to using the user-name
			// associated with the authentication token.
			else if(authenticationUsername != null) {
				username = authenticationUsername;
			}
			// It is illegal for this call to ever be in a state where an
			// authentication token and an authorization token were not given.
			else {
				throw
					new IllegalStateException(
						"A request that always requires authentication was " +
							"being processed without any authentication " +
							"information.");
			}
		}
		
		// If the authentication token was given and it refers to the user in
		// question, ensure that the token is valid.
		if(username.equals(authenticationUsername)) {
			// Ensure that the token has not expired.
			if(authenticationToken.getExpires() < System.currentTimeMillis()) {
				throw
					new InvalidAuthorizationException(
						"The authentication token has expired.");
			}
		}
		// If the authorization token was given and it refers to the user in
		// question, ensure that the token is valid and provides sufficient
		// privileges.
		else if(username.equals(authorizationUsername)) {
			// Ensure that the authorization token has not yet expired.
			if(authorizationToken.getExpirationTime() < System.currentTimeMillis()) {
				throw
					new InvalidAuthorizationException(
						"The authorization token has expired.");
			}
			// Ensure that the authorization token grants access to the 
			// requested schema.
			else if(
				! authorizationToken
					.getAuthorizationCode()
					.getScopes()
					.contains(schemaId)) {
				
				throw
					new InvalidAuthorizationException(
						"The given authorization token does not grant the " +
							"bearer access to the given schema ID.");
			}
			// TODO: Ensure that the code hasn't been invalidated.
			// TODO: Ensure that the token hasn't been invalidated.
			// TODO: Ensure that the token hasn't been refreshed, which
			// implicitly invalidates it.
		}
		// Otherwise, the given credentials were not sufficient for authorizing
		// the requester.
		else {
			throw
				new InvalidAuthorizationException(
					"Insufficient credentials were provided to read the " +
						"requested user's data.");
		}
		
		// Get the domain.
		String domain = parseDomain(schemaId);
		
		// Check to be sure the schema is known.
		if(
			(! ShimRegistry.hasDomain(domain)) &&
			(Registry
				.getInstance()
				.getSchemas(schemaId, version, 0, 1).count() == 0)) {
			
			throw
				new NoSuchSchemaException(
					"The schema ID, '" +
						schemaId +
						"', and version, '" +
						version +
						"', pair is unknown.");
		}
		
		// Get the data.
		MultiValueResult<Data> result;
		// Check if a shim should handle the request.
		if(ShimRegistry.hasDomain(domain)) {
			// Get the shim.
			Shim shim = ShimRegistry.getShim(domain);
			
			// Lookup the user's authorization code.
			ExternalAuthorizationToken token =
				ExternalAuthorizationTokenBin
					.getInstance()
					.getToken(username, domain);
			
			// If the token does not exist, return an error. Clients should
			// first check to be sure that the user has already authorized this
			// domain.
			if(token == null) {
				throw
					new OmhException(
						"The user has not yet authorized this domain.");
			}
			
			// Get the data from the shim.
			List<Data> resultList =
				shim
					.getData(
						schemaId,
						version,
						token,
						startDate,
						endDate,
						columnList,
						getNumToSkip(),
						getNumToReturn());
			
			// Convert the List object into a MultiValueResult object.
			result =
				(new MultiValueResultAggregator<Data>(resultList)).build();
		}
		// Otherwise, handle the request ourselves.
		else {
			// FIXME: Add the start and end date parameters.
			result =
				DataSet
					.getInstance()
					.getData(
						username, 
						schemaId, 
						version,
						startDate,
						endDate,
						columnList, 
						getNumToSkip(), 
						getNumToReturn());
		}
		
		// Set the meta-data.
		Map<String, Object> metaData = new HashMap<String, Object>();
		metaData.put(METADATA_KEY_COUNT, result.count());
		setMetaData(metaData);
		
		// Set the data.
		setData(result);
	}

	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.request.ListRequest#getPreviousNextParameters()
	 */
	@Override
	public Map<String, String> getPreviousNextParameters() {
		// Create the result map.
		Map<String, String> result = new HashMap<String, String>();
		
		// Add the owner if it's not the requesting user.
		if(! authenticationToken.getUsername().equals(owner)) {
			result.put(Version1.PARAM_OWNER, owner);
		}
		
		// Add the columns if they were given.
		if((columnList != null) && (columnList.size() > 0)) {
			result.put(Version1.PARAM_COLUMN_LIST, columnList.toString());
		}
		
		// Return the map.
		return result;
	}
}