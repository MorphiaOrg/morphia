package dev.morphia.test.mapping.scan.base;

/**
 * Records whether the package scan ran static initializers it had no need to.
 */
public final class ScanProbe {
    public static volatile boolean nonEntityInitialized;
    public static volatile boolean entityInitialized;

    private ScanProbe() {
    }
}
