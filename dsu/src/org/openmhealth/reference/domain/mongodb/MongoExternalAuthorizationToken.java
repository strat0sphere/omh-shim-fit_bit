package org.openmhealth.reference.domain.mongodb;

import java.util.Map;

import org.mongojack.MongoCollection;
import org.mongojack.ObjectId;
import org.openmhealth.reference.data.ExternalAuthorizationTokenBin;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>
 * A MongoDB extension of the {@link ExternalAuthorizationToken} type.
 * </p>
 *
 * @author John Jenkins
 */
@MongoCollection(name = ExternalAuthorizationTokenBin.DB_NAME)
public class MongoExternalAuthorizationToken
	extends ExternalAuthorizationToken
	implements MongoDbObject {
	
	/**
	 * The ID for this class which is used for serialization. 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The database ID for this object.
	 */
	@ObjectId
	private final String dbId;
	
	/**
	 * Creates a new authorization token based on the information from an
	 * existing authorization token, either one provided by the external party
	 * or by retrieving the information from the database.
	 * 
	 * @param username
	 *        The user-name of the Open mHealth user whose account is linked to
	 *        the external party.
	 * 
	 * @param accessToken
	 *        The token used to make requests to the external party.
	 * 
	 * @param refreshToken
	 *        The token used to generate new access and refresh tokens once
	 *        these have expired.
	 * 
	 * @param expiration
	 *        The time at which this access token expires.
	 * 
	 * @param extras
	 *        Domain-specific information stored with this token.
	 * 
	 * @throws OmhException
	 *         A parameter was invalid.
	 */
	@JsonCreator
	public MongoExternalAuthorizationToken(
		@JsonProperty(DATABASE_FIELD_ID) final String dbId, 
		@JsonProperty(JSON_KEY_USERNAME) final String username,
		@JsonProperty(JSON_KEY_DOMAIN) final String domain,
		@JsonProperty(JSON_KEY_ACCESS_TOKEN) final String accessToken,
		@JsonProperty(JSON_KEY_REFRESH_TOKEN) final String refreshToken,
		@JsonProperty(JSON_KEY_EXPIRATION_TIME) final long expiration,
		@JsonProperty(JSON_KEY_EXTRAS) final Map<String, Object> extras)
		throws OmhException {

		super(username, domain, accessToken, refreshToken, expiration, extras);
		
		// Store the MongoDB ID.
		if(dbId == null) {
			throw new OmhException("The MongoDB ID is missing.");
		}
		else {
			this.dbId = dbId;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.data.mongodb.MongoDbObject#getDatabaseId()
	 */
	@Override
	public String getDatabaseId() {
		return dbId;
	}	
}