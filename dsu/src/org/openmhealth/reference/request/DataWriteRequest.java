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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openmhealth.reference.data.DataSet;
import org.openmhealth.reference.data.Registry;
import org.openmhealth.reference.domain.AuthenticationToken;
import org.openmhealth.reference.domain.Data;
import org.openmhealth.reference.domain.MultiValueResult;
import org.openmhealth.reference.domain.Schema;
import org.openmhealth.reference.domain.User;
import org.openmhealth.reference.exception.InvalidAuthenticationException;
import org.openmhealth.reference.exception.OmhException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>
 * Stores the given data.
 * </p>
 *
 * @author John Jenkins
 */
public class DataWriteRequest extends Request<Object> {
	/**
	 * The authentication token for the requesting user.
	 */
	private final AuthenticationToken authToken;
	/**
	 * The ID of the schema from which the data was generated.
	 */
	private final String schemaId;
	/**
	 * The version of the schema from which the data was generated.
	 */
	private final long version;
	/**
	 * The data to validate and store.
	 */
	private final String data;
	
	/**
	 * Creates a request to store some data.
	 * 
	 * @param authToken
	 *        The requesting user's authentication token.
	 * 
	 * @param schemaId
	 *        The ID of the schema which should be used to validate the data.
	 * 
	 * @param version
	 *        The version of the schema which should be used to validate the
	 *        data.
	 * 
	 * @param data
	 *        The data to validate and store.
	 * 
	 * @throws OmhException
	 *         A parameter was invalid.
	 */
	public DataWriteRequest(
		final AuthenticationToken authToken,
		final String schemaId,
		final long version,
		final String data)		
		throws OmhException {
		
		if(authToken == null) {
			throw
				new InvalidAuthenticationException(
					"The authentication token is missing.");
		}
		if(schemaId == null) {
			throw new OmhException("The schema ID is missing.");
		}
		if(data == null) {
			throw new OmhException("The data is missing.");
		}
		
		this.authToken = authToken;
		this.schemaId = schemaId;
		this.version = version;
		this.data = data;
	}

	/**
	 * Validates the data and, if valid, stores it.
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
		
		// Check to be sure the schema is known.
		MultiValueResult<? extends Schema> schemas = 
			Registry.getInstance().getSchemas(schemaId, version, 0, 1);
		if(schemas.count() == 0) {
			throw
				new OmhException(
					"The schema ID, '" +
						schemaId +
						"', and version, '" +
						version +
						"', pair is unknown.");
		}
		Schema schema = schemas.iterator().next();
		
		// Get the user that owns this token.
		User requestingUser = authToken.getUser();
		
		// Create the result list of data points.
		List<Data> dataPoints = new ArrayList<Data>();
		
		// Create a new ObjectMapper that will be used to convert the meta-data
		// node into a MetaData object.
		ObjectMapper mapper = new ObjectMapper();
		
		// Parse the data.
		List<Data.Builder> builders;
		try {
			builders =
				mapper
					.readValue(
						data,
						new TypeReference<List<Data.Builder>>(){});
		}
		catch(JsonParseException e) {
			throw new OmhException("The data was not valid JSON.", e);
		}
		catch(JsonProcessingException e) {
			throw new OmhException("The data was not valid JSON.", e);
		}
		catch(IOException e) {
			throw new OmhException("Could not read the data.", e);
		}

		// Validate and build each point.
		for(Data.Builder builder : builders) {
			builder.setOwner(requestingUser.getUsername());
			dataPoints.add(builder.build(schema));
		}
		
		// Store the data.
		DataSet.getInstance().storeData(dataPoints);
	}
}