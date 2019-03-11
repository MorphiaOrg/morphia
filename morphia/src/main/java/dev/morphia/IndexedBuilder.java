/*
 * Copyright 2016 MongoDB, Inc.
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

package dev.morphia;

import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.utils.IndexDirection;

@SuppressWarnings("deprecation")
class IndexedBuilder extends AnnotationBuilder<Indexed> implements Indexed {
    @Override
    public Class<Indexed> annotationType() {
        return Indexed.class;
    }

    @Override
    public IndexOptions options() {
        return get("options");
    }

    @Override
    public boolean background() {
        return get("background");
    }

    @Override
    public boolean dropDups() {
        return get("dropDups");
    }

    @Override
    public int expireAfterSeconds() {
        return get("expireAfterSeconds");
    }

    @Override
    public String name() {
        return get("name");
    }

    @Override
    public boolean sparse() {
        return get("sparse");
    }

    @Override
    public boolean unique() {
        return get("unique");
    }

    @Override
    public IndexDirection value() {
        return get("value");
    }

    IndexedBuilder options(final IndexOptions options) {
        put("options", options);
        return this;
    }

    IndexedBuilder background(final boolean background) {
        put("background", background);
        return this;
    }

    IndexedBuilder dropDups(final boolean dropDups) {
        put("dropDups", dropDups);
        return this;
    }

    IndexedBuilder expireAfterSeconds(final int expireAfterSeconds) {
        put("expireAfterSeconds", expireAfterSeconds);
        return this;
    }

    IndexedBuilder name(final String name) {
        put("name", name);
        return this;
    }

    IndexedBuilder sparse(final boolean sparse) {
        put("sparse", sparse);
        return this;
    }

    IndexedBuilder unique(final boolean unique) {
        put("unique", unique);
        return this;
    }

    IndexedBuilder value(final IndexDirection value) {
        put("value", value);
        return this;
    }

}
