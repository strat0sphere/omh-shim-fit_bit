package org.openmhealth.reference.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmhealth.reference.data.Registry;
import org.openmhealth.reference.domain.MultiValueResult;
import org.openmhealth.reference.domain.MultiValueResultAggregator;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.Shim;
import org.openmhealth.shim.ShimRegistry;

/**
 * <p>
 * Retrieves all of the known schema versions.
 * </p>
 *
 * @author John Jenkins
 */
public class SchemaVersionsRequest extends ListRequest<Long> {
	/**
	 * The unique identifier for the schema whose versions are being requested.
	 */
	private final String schemaId;

	/**
	 * Creates a request for the list of known versions for a given schema.
	 * 
	 * @param schemaId The schema ID.
	 * 
	 * @param numToSkip The number of schema versions to skip.
	 *
	 * @param numToReturn The number of schema versions to return.
	 * 
	 * @throws OmhException A parameter was invalid.
	 */
	public SchemaVersionsRequest(
		final String schemaId,
		final Long numToSkip,
		final Long numToReturn)
		throws OmhException {
		
		super(numToSkip, numToReturn);
		
		// Validate the schema ID.
		if(schemaId == null) {
			throw new OmhException("The schema ID is missing.");
		}
		else {
			this.schemaId = schemaId;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.request.Request#service()
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
		
		// Get the domain from the schema ID.
		String domain;
		try {
			domain = parseDomain(schemaId);
		}
		catch(IllegalArgumentException e) {
			throw new OmhException("The schema ID is invalid: " + schemaId, e);
		}

		// Get the schema versions.
		MultiValueResult<Long> result;
		// If it can be handled by a shim, use that.
		if(ShimRegistry.hasDomain(domain)) {
			// Get the shim.
			Shim shim = ShimRegistry.getShim(domain);
			
			// Get all of the visible versions.
			List<Long> versions = shim.getSchemaVersions(schemaId);
			
			// Sort the list of versions.
			Collections.sort(versions);
			
			// Generate the paged result.
			versions =
				versions
					.subList(
						(int) Math.min(getNumToSkip(), versions.size()),
						(int) Math.min(
							getNumToSkip() + getNumToReturn(),
							versions.size()));
			
			// Create the result from the sorted, paged list of versions.
			result = (new MultiValueResultAggregator<Long>(versions)).build(); 
		}
		// Otherwise, query our internal schemas.
		else {
			result =
				Registry
					.getInstance()
						.getSchemaVersions(
							schemaId, 
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
		return Collections.emptyMap();
	}
}