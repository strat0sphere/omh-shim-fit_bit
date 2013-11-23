package org.openmhealth.shim;

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import name.jenkins.paul.john.concordia.Concordia;
import name.jenkins.paul.john.concordia.schema.ObjectSchema;
import name.jenkins.paul.john.concordia.validator.ValidationController;

import org.joda.time.DateTime;
import org.openmhealth.reference.domain.ColumnList;
import org.openmhealth.reference.domain.Data;
import org.openmhealth.reference.domain.ExternalAuthorizationToken;
import org.openmhealth.reference.domain.MetaData;
import org.openmhealth.reference.domain.Schema;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.shim.authorization.ShimAuthorization;
import org.openmhealth.shim.exception.ShimDataException;
import org.openmhealth.shim.exception.ShimSchemaException;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BlueButtonPlusShim implements Shim {
    private static final String DOMAIN = "blue_button_plus";

    /**
     * The prefix for all schemas used in this shim.
     */
    private static final String SCHEMA_PREFIX = "omh:" + DOMAIN + ":";

    private static final String DATA_TYPE_DEMOGRAPHICS = "demographics";
    private static final String DATA_TYPE_MEDICATIONS = "medications";

    public String getDomain() {
        return DOMAIN;
    }

	public ShimAuthorization getAuthorizationImplementation() {
        return new BlueButtonPlusShimAuthorization();
    }

	public List<String> getSchemaIds() {
        return Arrays.asList(
            SCHEMA_PREFIX + DATA_TYPE_DEMOGRAPHICS,
            SCHEMA_PREFIX + DATA_TYPE_MEDICATIONS);
    }

	public List<Long> getSchemaVersions(
		final String id)
		throws ShimSchemaException {
        // This shim doesn't (yet) have more than one version of a schema for
        // anything, so we just hardcode the return here.
        List<Long> versionList = new ArrayList<Long>();
        versionList.add(1L);
        return versionList;
    }

	public Schema getSchema(
		final String id,
		final Long version)
		throws ShimSchemaException {
        return ShimUtil.getSchema(id, version);
    }

	public List<Data> getData(
		final String schemaId,
		final Long version,
		final ExternalAuthorizationToken token,
		final DateTime startDate,
		final DateTime endDate,
		final ColumnList columnList,
		final Long numToSkip,
		final Long numToReturn)
		throws ShimDataException {
        // We only have a version 1 for now, so return null early for anything
        // but 1.
        if (!version.equals(1L)) {
            return null;
        }
        return null;
    }
}
