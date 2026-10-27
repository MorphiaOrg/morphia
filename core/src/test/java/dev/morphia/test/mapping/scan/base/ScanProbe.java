package dev.morphia.test.mapping.scan.base;

/**
 * Records whether the package scan ran a static initializer it had no need to.
 */
public final class ScanProbe {
    public static volatile boolean initialized;

    private ScanProbe() {
    }
}
