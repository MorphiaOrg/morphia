package dev.morphia.critter.parser;

import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.ReflectiveMapper;

/**
 * Test helper providing a default {@link Mapper} instance for use in critter tests.
 */
public class GeneratorsTestHelper {
    private static volatile Mapper instance;

    private GeneratorsTestHelper() {
    }

    /** Returns a lazily-initialized default {@link Mapper} for use in tests. */
    public static Mapper defaultMapper() {
        if (instance == null) {
            synchronized (GeneratorsTestHelper.class) {
                if (instance == null) {
                    instance = new ReflectiveMapper(new ManualMorphiaConfig());
                }
            }
        }
        return instance;
    }
}
