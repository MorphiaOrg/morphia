package dev.morphia.test.config;

import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.config.MorphiaConfig;

public interface MorphiaTestConfig extends MorphiaConfig {
    @Nullable
    List<Class<?>> classes();

    MorphiaTestConfig classes(List<Class<?>> value);

}
