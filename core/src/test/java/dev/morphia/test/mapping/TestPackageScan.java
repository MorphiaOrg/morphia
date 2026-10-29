package dev.morphia.test.mapping;

import dev.morphia.mapping.Mapper;
import dev.morphia.test.TestBase;
import dev.morphia.test.mapping.scan.base.ScanProbe;
import dev.morphia.test.mapping.scan.sub.ScanChild;
import dev.morphia.test.mapping.scan.sub.ScanTaggedImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPackageScan extends TestBase {
    private static final String SUB = "dev.morphia.test.mapping.scan.sub";

    @Test
    public void mapsClassesAnnotatedOutsideThePackage() {
        Mapper mapper = getMapper();
        mapper.map(SUB);

        Assertions.assertTrue(mapper.isMapped(ScanChild.class), "@Entity is inherited from a superclass in another package");
        Assertions.assertTrue(mapper.isMapped(ScanTaggedImpl.class),
                "@Entity is on an interface two levels up, in another package");
        Assertions.assertFalse(mapper.isMapped(className(SUB + ".NotAnEntity")));
    }

    @Test
    public void doesNotInitializeScannedClasses() {
        getMapper().map(SUB);

        Assertions.assertTrue(getMapper().isMapped(ScanChild.class));
        Assertions.assertFalse(ScanProbe.entityInitialized, "mapping an entity shouldn't run its static initializer");
        Assertions.assertFalse(ScanProbe.nonEntityInitialized, "the scan shouldn't run static initializers of classes it skips");
    }

    private static Class<?> className(String name) {
        try {
            // without initializing, which would defeat the test above
            return Class.forName(name, false, TestPackageScan.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
