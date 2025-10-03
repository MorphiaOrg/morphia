package dev.morphia.test;

import dev.morphia.config.MorphiaConfig;

/**
 * Interface for test classes that need to provide their own custom MorphiaConfig.
 * <p>
 * Test classes implementing this interface should be annotated with @CustomMorphiaConfig
 * to indicate they provide custom configuration to the MorphiaJUnitExtension.
 */
public interface MorphiaConfigProvider {

    /**
     * Provides the custom MorphiaConfig for this test class.
     * This method will be called during the beforeAll phase of the JUnit extension.
     *
     * @return the custom MorphiaConfig to use for this test class
     */
    MorphiaConfig provideMorphiaConfig();
}