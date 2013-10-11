package org.openmhealth.reference.data.mongodb;

import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.openmhealth.reference.data.ExternalAuthorizationTokenBin;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.domain.mongodb.MongoExternalAuthorizationToken;
import org.openmhealth.reference.exception.OmhException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

/**
 * <p>
 * The MongoDB implementation of the {@link ExternalAuthorizationTokenBin}.
 * </p>
 *
 * @author John Jenkins
 */
public class MongoExternalAuthorizationTokenBin
	extends ExternalAuthorizationTokenBin {
	
	/**
	 * Default constructor.
	 */
	protected MongoExternalAuthorizationTokenBin() {
		// Get the collection to add indexes to.
		DBCollection collection =
			MongoDao.getInstance().getDb().getCollection(DB_NAME);

		// Ensure that there is a compound key on the user-name and domain.
		collection
			.ensureIndex(
				(new BasicDBObject(
						ExternalAuthorizationToken.JSON_KEY_USERNAME,
						1))
					.append(ExternalAuthorizationToken.JSON_KEY_DOMAIN, 1),
				DB_NAME + 
					"_" + 
					ExternalAuthorizationToken.JSON_KEY_USERNAME + 
					"_" + 
					ExternalAuthorizationToken.JSON_KEY_DOMAIN,
				false);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.data.ExternalAuthorizationTokenBin#storeToken(org.openmhealth.reference.domain.ExternalAuthorizationToken)
	 */
	@Override
	public void storeToken(
		final ExternalAuthorizationToken token)
		throws OmhException {
		
		// Validate the parameter.
		if(token == null) {
			throw new OmhException("The token is null.");
		}
		
		// Get the external authorization token collection.
		JacksonDBCollection<ExternalAuthorizationToken, Object> collection =
			JacksonDBCollection
				.wrap(
					MongoDao
						.getInstance()
						.getDb()
						.getCollection(DB_NAME),
					ExternalAuthorizationToken.class);
		
		// Add the token.
		collection.insert(token);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.data.ExternalAuthorizationTokenBin#getToken(java.lang.String, java.lang.String)
	 */
	@Override
	public ExternalAuthorizationToken getToken(
		final String username,
		final String domain)
		throws OmhException {
		
		// Validate the parameters.
		if(username == null) {
			throw new OmhException("The username is null.");
		}
		if(domain == null) {
			throw new OmhException("The domain is null.");
		}

		// Get the external authorization token collection.
		JacksonDBCollection<MongoExternalAuthorizationToken, Object> collection =
			JacksonDBCollection
				.wrap(
					MongoDao
						.getInstance()
						.getDb()
						.getCollection(DB_NAME),
					MongoExternalAuthorizationToken.class);
		
		// Build the query.
		QueryBuilder queryBuilder = QueryBuilder.start();
		
		// Add the user-name.
		queryBuilder
			.and(ExternalAuthorizationToken.JSON_KEY_USERNAME)
			.is(username);
		
		// Add the domain.
		queryBuilder
			.and(ExternalAuthorizationToken.JSON_KEY_DOMAIN)
			.is(domain);
		
		// Create the sort parameters.
		DBObject sort =
			new BasicDBObject(
				ExternalAuthorizationToken.JSON_KEY_EXPIRATION_TIME,
				-1);
		
		// Execute query.
		DBCursor<MongoExternalAuthorizationToken> result =
			collection.find(queryBuilder.get()).sort(sort).limit(1);
		
		// If no results were returned, return null.
		if(result.size() == 0) {
			return null;
		}
		// Otherwise, return that result.
		else {
			return result.next();
		}
	}
}