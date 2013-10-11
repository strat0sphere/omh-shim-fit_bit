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

import java.util.Map;

import org.openmhealth.reference.exception.OmhException;

/**
 * <p>
 * The root class for all requests.
 * </p>
 * 
 * @author John Jenkins
 */
public abstract class Request<T> {
	/**
	 * The meta-data to be returned. This won't be generated until after this
	 * request was serviced.
	 */
	private Map<String, Object> metaData = null;
	/**
	 * The data to be returned. This won't be generated until after this
	 * request was serviced.
	 */
	private T data = null;

	/**
	 * An internal state variable
	 */
	private boolean serviced = false;

	/**
	 * Creates the base part of the request.
	 */
	public Request() {
		// Do nothing.
	}

	/**
	 * Returns a modifiable map representing the meta-data produced by
	 * servicing this request.
	 * 
	 * @return A map of the meta-data produced by servicing this request. This
	 *         is modifiable, so great care should be taken with the resulting
	 *         map.
	 */
	public Map<String, Object> getMetaData() {
		return metaData;
	}

	/**
	 * Returns the data produced by servicing this request.
	 * 
	 * @return The data produced by servicing this request or null if the
	 *         request has not yet been serviced.
	 */
	public T getData() {
		return data;
	}
	
	/**
	 * Parses a given schema ID and returns the domain. The domain is the part
	 * after the "omh:" (which is how all schema IDs should begin) and up until
	 * the next ":".
	 * 
	 * @param schemaId
	 *        The schema ID to parse.
	 * 
	 * @return The domain part of the schema ID.
	 * 
	 * @throws IllegalArgumentException
	 *         The schema ID is not valid schema ID.
	 */
	public String parseDomain(final String schemaId)
		throws IllegalArgumentException {
		
		// Validate the input.
		if(schemaId == null) {
			throw new IllegalArgumentException("The schema ID is null.");
		}
		
		// Ensure that the schema ID begins with the required "omh:".
		if(! schemaId.startsWith("omh:")) {
			throw
				new IllegalArgumentException(
					"The schema ID is not valid. It must begin with " +
						"\"omh:\".");
		}
		
		// Ensure that there is at least one character after the prefix.
		if(schemaId.length() <= 4) {
			throw
				new IllegalArgumentException(
					"The schema must include a value after the \"omh:\" " +
						"prefix.");
		}
		
		// Get the index of the colon immediately after the domain or just the
		// end of the string if no such colon exists.
		int domainColon = schemaId.indexOf(':', 4);
		if(domainColon == -1) {
			domainColon = schemaId.length();
		}
		
		// Create a sub-string that is the domain.
		return schemaId.substring(4, domainColon);
	}

	/**
	 * Returns whether or not this request has already been serviced.
	 * 
	 * @return Whether or not this request has already been serviced.
	 */
	protected boolean isServiced() {
		return serviced;
	}

	/**
	 * Sets this request as already having been serviced.
	 */
	protected void setServiced() {
		serviced = true;
	}

	/**
	 * <p>
	 * Sets the meta-data for this request.
	 * </p>
	 * 
	 * <p>
	 * An empty map indicates that an empty map may be serialized at some
	 * point; whereas, null indicates that any serialization should completely
	 * omit the empty map.
	 * </p>
	 * 
	 * <p>
	 * This should probably be used in conjunction with {@link #getMetaData()}
	 * to preserve any previously added elements.
	 * </p>
	 * 
	 * @param metaData
	 *        The meta-data that will completely replace the existing meta-data
	 *        map.
	 */
	protected void setMetaData(final Map<String, Object> metaData) {
		this.metaData = metaData;
	}

	/**
	 * Sets the data for this request after it has been serviced.
	 * 
	 * @param data
	 *        The data resulting from processing this request.
	 * 
	 * @throws OmhException
	 *         There was an error executing the request.
	 */
	protected void setData(final T data) throws OmhException {
		if(data == null) {
			throw new OmhException("The data is null.");
		}

		this.data = data;
	}

	/**
	 * Services the request and returns the domain-specific result. If the
	 * request encounters any errors, a {@link OmhException} should be
	 * thrown. If successful, any applicable data or meta-data should be set
	 * using the internal setter methods.
	 * 
	 * @throws OmhException
	 *         There was an error processing the request.
	 *         
	 * @see #setData(Object)
	 * @see #setMetaData(Map)
	 */
	public abstract void service() throws OmhException;
}