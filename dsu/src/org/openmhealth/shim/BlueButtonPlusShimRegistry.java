package org.openmhealth.shim;

public class BlueButtonPlusShimRegistry extends ShimRegistry {
    public Shim getShim() {
        return new BlueButtonPlusShim();
    }
}
