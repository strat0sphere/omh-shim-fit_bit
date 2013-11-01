package org.openmhealth.reference.data.mongodb;

import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.mongojack.internal.MongoJackModule;
import org.openmhealth.reference.data.ExternalAuthorizationInformationBin;
import org.openmhealth.reference.domain.ExternalAuthorizationInformation;
import org.openmhealth.reference.domain.mongodb.MongoExternalAuthorizationInformation;
import org.openmhealth.reference.exception.OmhException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoException.DuplicateKey;
import com.mongodb.QueryBuilder;

/**
 * <p>
 * The MongoDB implementation of the
 * {@link ExternalAuthorizationInformationBin} class.
 * </p>
 *
 * @author John Jenkins
 */
public class MongoExternalAuthorizationInformationBin
	extends ExternalAuthorizationInformationBin {
	
	/**
	 * The object mapper that should be used to parse
	 * {@link ExternalAuthorizationInformation} objects.
	 */
	private static final ObjectMapper JSON_MAPPER;
	static {
		// Create the object mapper.
		ObjectMapper mapper = new ObjectMapper();
		
		// Create the FilterProvider.
		SimpleFilterProvider filterProvider = new SimpleFilterProvider();
		filterProvider.setFailOnUnknownId(false);
		mapper.setFilters(filterProvider);
		
		// Finally, we must configure the mapper to work with the MongoJack
		// configuration.
		JSON_MAPPER = MongoJackModule.configure(mapper);
	}	
	
	/**
	 * Default constructor.
	 */
	protected MongoExternalAuthorizationInformationBin() {
		// Get the collection to add indexes to.
		DBCollection collection =
			MongoDao.getInstance().getDb().getCollection(DB_NAME);
		
		// Ensure that there is a unique key on the authorize ID.
		collection
			.ensureIndex(
				(new BasicDBObject(
						ExternalAuthorizationInformation.JSON_KEY_AUTHORIZE_ID,
						1)),
				DB_NAME + 
					"_" + 
					ExternalAuthorizationInformation.JSON_KEY_AUTHORIZE_ID + 
					"_" + 
					"unique",
				true);

		// Ensure that there is a compound key on the user-name and domain.
		collection
			.ensureIndex(
				(new BasicDBObject(
						ExternalAuthorizationInformation.JSON_KEY_USERNAME,
						1))
					.append(
						ExternalAuthorizationInformation.JSON_KEY_DOMAIN,
						1),
				DB_NAME + 
					"_" + 
					ExternalAuthorizationInformation.JSON_KEY_USERNAME + 
					"_" + 
					ExternalAuthorizationInformation.JSON_KEY_DOMAIN,
				false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.data.ExternalAuthorizationInformationBin#storeInformation(org.openmhealth.reference.domain.ExternalAuthorizationInformation)
	 */
	@Override
	public void storeInformation(
		final ExternalAuthorizationInformation information)
		throws OmhException {
		
		// Validate the parameter.
		if(information == null) {
			throw new OmhException("The information is null.");
		}
		
		// Get the external authorization information collection.
		JacksonDBCollection<ExternalAuthorizationInformation, Object> collection =
			JacksonDBCollection
				.wrap(
					MongoDao
						.getInstance()
						.getDb()
						.getCollection(DB_NAME),
					ExternalAuthorizationInformation.class,
					Object.class,
					JSON_MAPPER);
		
		// Add the token.
		try {
			collection.insert(information);
		}
		catch(DuplicateKey e) {
			throw
				new OmhException(
					"Information with the same authorize ID already exists.",
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.data.ExternalAuthorizationInformationBin#informationExists(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean informationExists(
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
		
		// Get the external authorization information collection.
		JacksonDBCollection<MongoExternalAuthorizationInformation, Object> collection =
			JacksonDBCollection
				.wrap(
					MongoDao
						.getInstance()
						.getDb()
						.getCollection(DB_NAME),
					MongoExternalAuthorizationInformation.class,
					Object.class,
					JSON_MAPPER);
		
		// Build the query.
		QueryBuilder queryBuilder = QueryBuilder.start();
		
		// Add the user-name.
		queryBuilder
			.and(ExternalAuthorizationInformation.JSON_KEY_USERNAME)
			.is(username);
		
		// Add the domain.
		queryBuilder
			.and(ExternalAuthorizationInformation.JSON_KEY_DOMAIN)
			.is(domain);
		
		// Perform the query and limit the results to just one. Then, get the
		// count and return it.
		return (collection.find(queryBuilder.get()).limit(1).size() == 1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.data.ExternalAuthorizationInformationBin#getInformation(java.lang.String)
	 */
	@Override
	public ExternalAuthorizationInformation getInformation(
		final String authorizeId)
		throws OmhException {

		// Validate the parameter.
		if(authorizeId == null) {
			throw new OmhException("The authorize ID is null.");
		}
		
		// Get the external authorization information collection.
		JacksonDBCollection<MongoExternalAuthorizationInformation, Object> collection =
			JacksonDBCollection
				.wrap(
					MongoDao
						.getInstance()
						.getDb()
						.getCollection(DB_NAME),
					MongoExternalAuthorizationInformation.class,
					Object.class,
					JSON_MAPPER);
		
		// Perform the query.
		DBCursor<MongoExternalAuthorizationInformation> result =
			collection
				.find(
					new BasicDBObject(
						ExternalAuthorizationInformation.JSON_KEY_AUTHORIZE_ID,
						authorizeId));
		
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