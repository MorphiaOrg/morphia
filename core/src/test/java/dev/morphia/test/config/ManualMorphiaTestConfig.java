package dev.morphia.test.config;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;

public class ManualMorphiaTestConfig extends ManualMorphiaConfig implements MorphiaTestConfig {
    List<Class<?>> classes;

    public ManualMorphiaTestConfig() {
        this(MorphiaConfig.load());
    }

    public ManualMorphiaTestConfig(MorphiaConfig config) {
        super(config);
        if (config instanceof ManualMorphiaTestConfig) {
            classes = ((ManualMorphiaTestConfig) config).classes;
        }
    }

    public List<Class<?>> classes() {
        return classes;
    }

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    public MorphiaTestConfig classes(List<Class<?>> value) {
        var newConfig = new ManualMorphiaTestConfig(this);

        newConfig.classes = new ArrayList<>(value);
        newConfig.packages(new ArrayList<>());
        return newConfig;
    }
}
