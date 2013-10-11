package org.openmhealth.reference.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
 * Retrieves all of the known schema IDs.
 * </p>
 *
 * @author John Jenkins
 */
public class SchemaIdsRequest extends ListRequest<String> {
	/**
	 * Creates a request for the list of known schema IDs.
	 * 
	 * @param numToSkip The number of schema IDs to skip.
	 * 
	 * @param numToReturn The number of schema IDs to return.
	 * 
	 * @throws OmhException A parameter was invalid.
	 */
	public SchemaIdsRequest(
		final Long numToSkip,
		final Long numToReturn)
		throws OmhException {
		
		super(numToSkip, numToReturn);
	}

	/**
	 * Retrieves the list of known schema IDs.
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
		
		// Get the number of records to skip and the number to return and store
		// them as we will need to temporarily modify them.
		long currNumToSkip = getNumToSkip();
		long currNumToReturn = getNumToReturn();
		long totalSchemasNeeded = currNumToSkip + currNumToReturn;
		
		// Get the set of known domains and convert it into a list for sorting.
		List<String> domains =
			new ArrayList<String>(ShimRegistry.getDomains());
		Collections.sort(domains);
		
		// For each domain, get the list of known schema IDs.
		Iterator<String> domainIterator = domains.iterator();
		List<String> externalSchemaIds = new LinkedList<String>();
		while(
			domainIterator.hasNext() &&
			(externalSchemaIds.size() < totalSchemasNeeded)) {
			
			Shim shim = ShimRegistry.getShim(domainIterator.next());
			externalSchemaIds.addAll(shim.getSchemaIds());
		}
		
		// Save the number of external schema IDs.
		int numExternalSchemaIds = externalSchemaIds.size();
		
		// Remove the schema IDs that should be skipped and limit it by the
		// number that should be returned.
		externalSchemaIds =
			externalSchemaIds
				.subList(
					(int) currNumToSkip,
					(int)
						Math.min(
							externalSchemaIds.size(),
							totalSchemasNeeded));
		
		// Compute the number of local schema IDs to skip and return.
		if(numExternalSchemaIds >= totalSchemasNeeded) {
			currNumToSkip = 0;
			currNumToReturn = 0;
		}
		else if(numExternalSchemaIds <= currNumToSkip) {
			currNumToSkip -= numExternalSchemaIds;
		}
		else {
			long overflow = numExternalSchemaIds - currNumToSkip;
			currNumToSkip = 0;
			currNumToReturn -= overflow;
		}

		// Get the schema IDs.
		MultiValueResult<String> internalSchemaIds =
			Registry
				.getInstance().getSchemaIds(currNumToSkip, currNumToReturn);
		
		// Aggregate the two lists.
		MultiValueResult<String> result =
			(new MultiValueResultAggregator<String>(externalSchemaIds))
			.add(internalSchemaIds)
			.build();
		
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