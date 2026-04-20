package dev.morphia.test.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.config.MorphiaPropertyAnnotationProvider;
import dev.morphia.config.PropertyAnnotationProvider;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestPropertyAnnotationProviders {

    /** Dummy annotation for a test provider. */
    @Retention(RetentionPolicy.RUNTIME)
    @interface CustomAnnotation {
    }

    static class CustomProvider implements PropertyAnnotationProvider<CustomAnnotation> {
        @Override
        public Class<CustomAnnotation> provides() {
            return CustomAnnotation.class;
        }
    }

    @Test
    public void defaultConfigContainsMorphiaProvider() {
        List<PropertyAnnotationProvider<?>> providers = new ManualMorphiaConfig().propertyAnnotationProviders();

        assertEquals(providers.size(), 1, "Default config should have exactly one provider");
        assertTrue(providers.get(0) instanceof MorphiaPropertyAnnotationProvider,
                "Default provider should be MorphiaPropertyAnnotationProvider");
    }

    @Test
    public void loadedConfigContainsMorphiaProvider() {
        // SmallRye Config path — uses @WithDefault
        List<PropertyAnnotationProvider<?>> providers = MorphiaConfig.load().propertyAnnotationProviders();

        assertFalse(providers.isEmpty(), "Loaded config should have at least one provider");
        assertTrue(providers.stream().findFirst().stream().allMatch(p -> p instanceof MorphiaPropertyAnnotationProvider),
                "Loaded config should always include MorphiaPropertyAnnotationProvider");
    }

    @Test
    public void customProviderIncludesMorphiaProvider() {
        MorphiaConfig config = new ManualMorphiaConfig()
                .propertyAnnotationProviders(List.of(new CustomProvider()));
        List<PropertyAnnotationProvider<?>> providers = config.propertyAnnotationProviders();

        assertTrue(providers.stream().anyMatch(p -> p instanceof MorphiaPropertyAnnotationProvider),
                "MorphiaPropertyAnnotationProvider must always be present");
        assertTrue(providers.stream().anyMatch(p -> p instanceof CustomProvider),
                "CustomProvider should be present");
    }

    @Test
    public void customProviderNoDuplication() {
        MorphiaConfig config = new ManualMorphiaConfig()
                .propertyAnnotationProviders(List.of(new CustomProvider()));
        List<PropertyAnnotationProvider<?>> providers = config.propertyAnnotationProviders();

        long morphiaCount = providers.stream().filter(p -> p instanceof MorphiaPropertyAnnotationProvider).count();
        assertEquals(morphiaCount, 1, "MorphiaPropertyAnnotationProvider must appear exactly once");
        assertEquals(providers.size(), 2, "Should have exactly MorphiaPropertyAnnotationProvider + CustomProvider");
    }

    @Test
    public void customProviderSurvivedChainedSetter() {
        // Each chained setter call creates a new ManualMorphiaConfig via the copy constructor.
        // Verify that custom providers are not lost when other settings are changed afterwards.
        MorphiaConfig config = new ManualMorphiaConfig()
                .propertyAnnotationProviders(List.of(new CustomProvider()))
                .database("mydb");
        List<PropertyAnnotationProvider<?>> providers = config.propertyAnnotationProviders();

        assertTrue(providers.stream().anyMatch(p -> p instanceof CustomProvider),
                "CustomProvider must survive chained setter calls");
        assertTrue(providers.stream().anyMatch(p -> p instanceof MorphiaPropertyAnnotationProvider),
                "MorphiaPropertyAnnotationProvider must survive chained setter calls");
        assertEquals(providers.size(), 2);
    }

    @Test
    public void explicitMorphiaProviderNoDuplication() {
        // User explicitly passes MorphiaPropertyAnnotationProvider in their list
        MorphiaConfig config = new ManualMorphiaConfig()
                .propertyAnnotationProviders(List.of(new MorphiaPropertyAnnotationProvider(), new CustomProvider()));
        List<PropertyAnnotationProvider<?>> providers = config.propertyAnnotationProviders();

        long morphiaCount = providers.stream().filter(p -> p instanceof MorphiaPropertyAnnotationProvider).count();
        assertEquals(morphiaCount, 1, "MorphiaPropertyAnnotationProvider must appear exactly once even when explicitly provided");
        assertEquals(providers.size(), 2, "Should have exactly MorphiaPropertyAnnotationProvider + CustomProvider");
    }
}
