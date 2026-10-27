package dev.morphia.test.mapping.scan.sub;

import dev.morphia.test.mapping.scan.base.ScanProbe;

public class NotAnEntity {
    static {
        ScanProbe.nonEntityInitialized = true;
    }
}
