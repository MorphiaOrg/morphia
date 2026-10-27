package dev.morphia.test.mapping.scan.sub;

import dev.morphia.test.mapping.scan.base.ScanBase;
import dev.morphia.test.mapping.scan.base.ScanProbe;

/**
 * Not annotated itself: mappable only through {@code @Entity} on a superclass in another package.
 */
public class ScanChild extends ScanBase {
    static {
        ScanProbe.entityInitialized = true;
    }

    public String name;
}
