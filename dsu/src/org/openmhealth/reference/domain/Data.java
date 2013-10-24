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
package org.openmhealth.reference.domain;

import org.openmhealth.reference.exception.OmhException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>
 * A data point as defined by the Open mHealth specification.
 * </p>
 * 
 * <p>
 * This class is immutable.
 * </p>
 * 
 * @author John Jenkins
 */
public class Data implements OmhObject {
	/**
	 * <p>
	 * A builder for {@link Data} objects.
	 * </p>
	 *
	 * @author John Jenkins
	 */
	public static class Builder {
		private String owner;
		private MetaData metaData;
		private JsonNode data;
		
		/**
		 * Builds a new Builder object.
		 * 
		 * @param metaData
		 *        The meta-data about the point.
		 * 
		 * @param data
		 *        The point's data.
		 */
		@JsonCreator
		public Builder(
			@JsonProperty(JSON_KEY_METADATA) final MetaData metaData,
			@JsonProperty(JSON_KEY_DATA) final JsonNode data) {
			
			this.metaData = metaData;
			this.data = data;
		}
		
		/**
		 * Sets the owner of the data point.
		 * 
		 * @param owner
		 *        The owner of the data point.
		 */
		public void setOwner(final String owner) {
			this.owner = owner;
		}
		
		/**
		 * Uses a Schema to validate some data and, if successful, creates a
		 * Data object.
		 * 
		 * @param schema
		 *        The schema to use to validate the new Data point.
		 * 
		 * @return The validated data point.
		 */
		public Data build(final Schema schema) {
			return schema.validateData(owner, metaData, data);
		}
	}
	
	/**
	 * The version of this class used for serialization purposes.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The JSON key for the identifier of a user that owns this data.
	 */
	public static final String JSON_KEY_OWNER = "owner";
	
	/**
	 * The JSON key for the meta-data.
	 */
	public static final String JSON_KEY_METADATA = "metadata";
	/**
	 * The JSON key for the data.
	 */
	public static final String JSON_KEY_DATA = "data";
	
	/**
	 * The identifier for the user that owns this data.
	 */
	@JsonProperty(JSON_KEY_OWNER)
	private final String owner;

	/**
	 * The unique identifier for the schema that validated this data. This
	 * should always exist, even if the schema is null.
	 */
	@JsonProperty(Schema.JSON_KEY_ID)
	private final String schemaId;
	/**
	 * The version of the schema that validated this data. This should always
	 * exist, even if the schema is null.
	 */
	@JsonProperty(Schema.JSON_KEY_VERSION)
	private final long schemaVersion;
	
	/**
	 * The meta-data for this point.
	 */
	@JsonProperty(JSON_KEY_METADATA)
	private final MetaData metaData;
	/**
	 * The data for this point.
	 */
	@JsonProperty(JSON_KEY_DATA)
	private final JsonNode data;
	
	/**
	 * Creates a new Data object.
	 * 
	 * @param owner
	 * 		  The identifier for the user that owns the data.
	 * 
	 * @param schemaId
	 * 		  The ID of the schema that was used to validate this data.
	 * 
	 * @param schemaVersion
	 * 		  The version of the schema that was used to validate this data.
	 * 
	 * @param metaData
	 *        The meta-data for this data.
	 * 
	 * @param data
	 *        The data.
	 * 
	 * @throws OmhException
	 *         Any of the parameters is null.
	 */
	@JsonCreator
	public Data(
		@JsonProperty(JSON_KEY_OWNER) final String owner,
		@JsonProperty(Schema.JSON_KEY_ID) final String schemaId,
		@JsonProperty(Schema.JSON_KEY_VERSION) final long schemaVersion,
		@JsonProperty(JSON_KEY_METADATA) final MetaData metaData,
		@JsonProperty(JSON_KEY_DATA) final JsonNode data)
		throws OmhException {
		
		if(owner == null) {
			throw new OmhException("The owner is null.");
		}
		if(schemaId == null) {
			throw new OmhException("The schema ID is null.");
		}
		if(data == null) {
			throw new OmhException("The data is null.");
		}
		
		this.owner = owner;
		this.schemaId = schemaId;
		this.schemaVersion = schemaVersion;
		this.metaData = metaData;
		this.data = data;
	}
	
	/**
	 * Returns the username of the owner of this data point.
	 * 
	 * @return The username of the owner of this data point.
	 */
	public String getOwner() {
		return owner;
	}
	
	/**
	 * Returns the ID of the schema to which this point is associated.
	 * 
	 * @return The ID of the schema to which this point is associated.
	 */
	public String getSchemaId() {
		return schemaId;
	}

	/**
	 * Returns the version of the schema to which this point is associated.
	 * 
	 * @return The version of the schema to which this point is associated.
	 */
	public long getSchemaVersion() {
		return schemaVersion;
	}

	/**
	 * Returns the meta-data associated with this point.
	 * 
	 * @return The meta-data associated with this point or null if there is no
	 *         meta-data.
	 */
	public MetaData getMetaData() {
		return metaData;
	}
	
	/**
	 * Returns the data associated with this point.
	 * 
	 * @return The data associated with this point.
	 */
	public JsonNode getData() {
		return data;
	}
}