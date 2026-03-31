package dev.morphia.critter.parser;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.config.MorphiaConfigHelper;
import dev.morphia.mapping.ReflectiveMapper;

/**
 * Test helper providing a default {@link Generators} instance backed by the default Morphia config.
 */
public class GeneratorsTestHelper {
    private static volatile Generators instance;

    private GeneratorsTestHelper() {
    }

    /** Returns a lazily-initialized default {@link Generators} instance for use in tests. */
    public static Generators defaultGenerators() {
        if (instance == null) {
            synchronized (GeneratorsTestHelper.class) {
                if (instance == null) {
                    MorphiaConfig config = MorphiaConfig.load(MorphiaConfigHelper.MORPHIA_CONFIG_PROPERTIES);
                    instance = new Generators(config, new ReflectiveMapper(config));
                }
            }
        }
        return instance;
    }
}
