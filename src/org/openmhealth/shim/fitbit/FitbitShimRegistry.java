package org.openmhealth.shim.fitbit;

import org.openmhealth.shim.Shim;
import org.openmhealth.shim.ShimRegistry;

public class FitbitShimRegistry extends ShimRegistry {
    public Shim getShim() {
        return new FitbitShim();
    }
}
