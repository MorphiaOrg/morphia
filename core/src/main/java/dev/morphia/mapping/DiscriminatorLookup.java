/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia.mapping;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.sofia.Sofia;

import org.bson.codecs.configuration.CodecConfigurationException;

import static java.lang.String.format;

/**
 * Provides lookup capabilities to find a type by its discriminator
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public final class DiscriminatorLookup {
    private final Map<String, Class<?>> discriminatorClassMap = new ConcurrentHashMap<>();
    private final Set<String> packages = new ConcurrentSkipListSet<>();
    private final ClassLoader classLoader;

    /**
     * Creates a new lookup
     *
     */
    public DiscriminatorLookup() {
        this.classLoader = Thread.currentThread().getContextClassLoader();

    }

    /**
     * Adds a model to the map
     *
     * @param entityModel the model
     */
    public void addModel(EntityModel entityModel) {
        String discriminator = entityModel.getDiscriminator();
        Class<?> current = discriminatorClassMap.put(discriminator, entityModel.getType());
        if (current != null) {
            throw new MappingException(Sofia.duplicateDiscriminators(discriminator, current.getName(),
                    entityModel.getType().getName()));
        }
    }

    /**
     * Looks up a discriminator value
     *
     * @param discriminator the value to search witih
     * @return the mapped class
     */
    public Class<?> lookup(String discriminator) {
        if (discriminatorClassMap.containsKey(discriminator)) {
            return discriminatorClassMap.get(discriminator);
        }

        Class<?> clazz = getClassForName(discriminator);
        if (clazz == null) {
            clazz = searchPackages(discriminator);
        }

        if (clazz == null) {
            throw new CodecConfigurationException(format("A class could not be found for the discriminator: '%s'.", discriminator));
        }
        return clazz;
    }

    @Nullable
    private Class<?> getClassForName(String discriminator) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(discriminator, true, classLoader);
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        return clazz;
    }

    @Nullable
    private Class<?> searchPackages(String discriminator) {
        Class<?> clazz = null;
        for (String packageName : packages) {
            clazz = getClassForName(packageName + "." + discriminator);
            if (clazz != null) {
                return clazz;
            }
        }
        return clazz;
    }
}
