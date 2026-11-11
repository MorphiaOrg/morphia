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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    // the key each model that uses a discriminator stores it under, by discriminator, and the keys themselves
    private final Map<String, String> keysByDiscriminator = new ConcurrentHashMap<>();
    private final Set<String> keys = ConcurrentHashMap.newKeySet();
    // Class.forName results for discriminators no model registered, including misses, so a stored value that names no
    // class (or names one under a since-changed discriminator style) doesn't reload or rethrow per document
    private static final int MAX_CACHED_NAMES = 1024;
    private final Map<String, Optional<Class<?>>> classesByName = new ConcurrentHashMap<>();
    private final ClassLoader classLoader;

    /**
     * Creates a new lookup
     *
     */
    public DiscriminatorLookup() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates a new lookup with the given ClassLoader.
     *
     * @param classLoader the ClassLoader to use for class resolution
     */
    public DiscriminatorLookup(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Adds a model to the map
     *
     * @param entityModel the model
     */
    public void addModel(EntityModel entityModel) {
        String discriminator = entityModel.discriminator();
        Class<?> current = discriminatorClassMap.putIfAbsent(discriminator, entityModel.getType());
        if (current != null) {
            throw new MappingException(Sofia.duplicateDiscriminators(discriminator, current.getName(),
                    entityModel.getType().getName()));
        }
        if (entityModel.useDiscriminator()) {
            keysByDiscriminator.put(discriminator, entityModel.discriminatorKey());
            keys.add(entityModel.discriminatorKey());
        }
    }

    /**
     * @param configuredKey the discriminator key the configuration names
     * @param name          the name of a field in a stored document
     * @return true if a string stored under the name may be a discriminator: it's the configured key, or a key a mapped
     *         entity stores its own discriminator under
     */
    public boolean isKey(String configuredKey, String name) {
        return name.equals(configuredKey) || keys.contains(name);
    }

    /**
     * Resolves a discriminator found in a document whose type isn't otherwise known, such as an untyped value or an id
     * stored as a document. Under the configured key, it's looked up as {@link #lookup(String)} does. Under a key an
     * entity declares for itself, it must be that entity's discriminator, so an ordinary document that happens to have a
     * field of that name stays a document.
     *
     * @param configuredKey the discriminator key the configuration names
     * @param name          the name the value is stored under
     * @param value         the value
     * @return the class, or null if the value isn't a discriminator stored under its entity's own key
     * @throws CodecConfigurationException if it's under the configured key and no class can be found for it
     */
    @Nullable
    public Class<?> lookup(String configuredKey, String name, String value) {
        if (name.equals(configuredKey)) {
            return lookup(value);
        }
        return name.equals(keysByDiscriminator.get(value)) ? discriminatorClassMap.get(value) : null;
    }

    /**
     * Looks up a discriminator value
     *
     * @param discriminator the value to search witih
     * @return the mapped class
     */
    public Class<?> lookup(String discriminator) {
        Class<?> clazz = discriminatorClassMap.get(discriminator);
        if (clazz == null) {
            // not computeIfAbsent: loading initializes the class, whose initializer could come back here
            Optional<Class<?>> loaded = classesByName.get(discriminator);
            if (loaded == null) {
                loaded = getClassForName(discriminator);
                // stored data decides the values, so the cache is capped rather than growing with every stray one
                if (classesByName.size() < MAX_CACHED_NAMES) {
                    classesByName.putIfAbsent(discriminator, loaded);
                }
            }
            clazz = loaded.orElseThrow(() -> new CodecConfigurationException(
                    format("A class could not be found for the discriminator: '%s'.", discriminator)));
        }
        return clazz;
    }

    private Optional<Class<?>> getClassForName(String discriminator) {
        try {
            return Optional.of(Class.forName(discriminator, true, classLoader));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

}
