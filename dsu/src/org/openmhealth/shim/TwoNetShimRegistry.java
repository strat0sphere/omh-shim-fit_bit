package org.openmhealth.shim;

public class TwoNetShimRegistry extends ShimRegistry {
    public Shim getShim() {
        return new TwoNetShim();
    }
}
