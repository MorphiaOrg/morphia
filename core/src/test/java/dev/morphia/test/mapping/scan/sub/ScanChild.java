package dev.morphia.test.mapping.scan.sub;

import dev.morphia.test.mapping.scan.base.ScanBase;

/**
 * Not annotated itself: mappable only through {@code @Entity} on a superclass in another package.
 */
public class ScanChild extends ScanBase {
    public String name;
}
