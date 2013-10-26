package org.openmhealth.shim;

public class WithingsShimRegistry extends ShimRegistry {
    public Shim getShim() {
        return new WithingsShim();
    }
}
