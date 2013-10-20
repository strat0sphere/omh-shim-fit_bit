package org.openmhealth.shim;

public class FitbitShimRegistry extends ShimRegistry {
    public Shim getShim() {
        return new FitbitShim();
    }
}
