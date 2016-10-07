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

package org.mongodb.morphia;

import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.utils.IndexDirection;

class IndexedBuilder extends AnnotationBuilder<Indexed> {
    @Override
    public Class<Indexed> annotationType() {
        return Indexed.class;
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
