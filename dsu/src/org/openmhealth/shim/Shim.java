package org.openmhealth.shim;

import java.util.List;

import org.joda.time.DateTime;
import org.openmhealth.reference.data.ExternalAuthorizationTokenBin;
import org.openmhealth.reference.domain.ColumnList;
import org.openmhealth.reference.domain.Data;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.domain.Schema;
import org.openmhealth.shim.authorization.ShimAuthorization;
import org.openmhealth.shim.exception.ShimDataException;
import org.openmhealth.shim.exception.ShimSchemaException;

/**
 * <p>
 * An interface that describes the necessary functionality of a shim. A shim is
 * a piece of software that can generate or translate the definitions and data
 * of a web service into the Open mHealth format.
 * </p>
 *
 * @author John Jenkins
 */
public interface Shim {
	/**
	 * The domain that this shim can handle. This should correspond directly to
	 * the types of schema IDs it should expect. For example, a shim to the
	 * FitBit domain should return "<tt>fitbit</tt>" and should expect schema
	 * IDs that begin with "<tt>omh:fitbit:</tt>".
	 * 
	 * @return The domain that this shim can handle.
	 */
	public String getDomain();
	
	/**
	 * Returns the authorization implementation used by this shim.
	 * 
	 * @return The authorization implementation used by this shim.
	 */
	public ShimAuthorization getAuthorizationImplementation();
	
	/**
	 * Returns the list of schema IDs that can be handled by this shim. All
	 * records should begin with: "<tt>omh:&lt;domain&gt;</tt>". This should
	 * never be null.
	 * 
	 * @return The list of schema IDs that can be handled by this shim.
	 * 
	 * @see #getDomain()
	 */
	public List<String> getSchemaIds();
	
	/**
	 * Returns the list of known versions of a given schema ID. If the ID is
	 * unknown, the returned list should be empty.
	 * 
	 * @param id
	 *        The schema ID whose versions are in question.
	 * 
	 * @return The known versions of this schema ID. The list should be empty
	 *         if the schema is unknown.
	 * 
	 * @throws ShimSchemaException
	 *         The ID is null.
	 */
	public List<Long> getSchemaVersions(
		final String id)
		throws ShimSchemaException;
	
	/**
	 * The schema for a given schema ID-version pair. If the ID is unknown or
	 * the version for the schema is unknown, null should be returned.
	 * 
	 * @param id
	 *        The schema ID.
	 * 
	 * @param version
	 *        The schema version.
	 * 
	 * @return The schema for the given schema-ID version pair or null if the
	 *         pair is unknown.
	 * 
	 * @throws ShimSchemaException
	 *         The ID and/or version are null.
	 */
	public Schema getSchema(
		final String id,
		final Long version)
		throws ShimSchemaException;
	
	/**
	 * <p>
	 * Returns a list of data points based on the parameters.
	 * </p>
	 * 
	 * <p>
	 * The token may need to be refreshed. This should be checked before
	 * attempting to use the token, and, if expired, the token should be
	 * refreshed. The resulting, refreshed token should be saved in the
	 * database using the
	 * {@link ExternalAuthorizationTokenBin#storeToken(ExternalAuthorizationToken)}
	 * method.
	 * </p>
	 * 
	 * @param schemaId
	 *        The schema ID. This should not be null.
	 * 
	 * @param version
	 *        The version of the schema based on the ID. This should not be
	 *        null.
	 * 
	 * @param token
	 *        The authorization token provided by the external resource after
	 *        the Open mHealth user granted this shim access to its data. This
	 *        should first be checked to guarantee that the token is valid and
	 *        refreshed if it is not. Then, it should be used to gather the
	 *        requested data.
	 * 
	 * @param startDate
	 *        The date and time of the earliest point. This may be null.
	 * 
	 * @param endDate
	 *        The date and time of the latest point. This may be null.
	 * 
	 * @param columnList
	 *        The list of columns to use to limit the data being returned.
	 * 
	 * @param numToSkip
	 *        The number of data points that adhere to the parameters of this
	 *        request that should be skipped. This implies some ordering on the
	 *        data such that, the same call being made to the same set of data
	 *        should result in the same data set being returned.
	 * 
	 * @param numToReturn
	 *        The number of data points to return after skipping has been
	 *        processed. For example, if a data set had 15 records that
	 *        conformed to the parameters in this call, the number to skip was
	 *        5, and this parameter was 7, then this call should return points
	 *        6, 7, 8, 9, 10, 11, and 12 (assuming the first point was point
	 *        1).
	 * 
	 * @return The list of data that conforms to the parameters as specified
	 *         above.
	 * 
	 * @throws ShimDataException
	 *         A parameter was invalid.
	 */
	public List<Data> getData(
		final String schemaId,
		final Long version,
		final ExternalAuthorizationToken token,
		final DateTime startDate,
		final DateTime endDate,
		final ColumnList columnList,
		final Long numToSkip,
		final Long numToReturn)
		throws ShimDataException;
}