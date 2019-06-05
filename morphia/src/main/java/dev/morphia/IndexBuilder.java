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

import dev.morphia.annotations.Field;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;

import java.util.List;

class IndexBuilder extends AnnotationBuilder<Index> implements Index {
    IndexBuilder() {
    }

    IndexBuilder(final Index original) {
        super(original);
    }

    @Override
    public Class<Index> annotationType() {
        return Index.class;
    }

    @Override
    public Field[] fields() {
        return get("fields");
    }

    @Override
    public IndexOptions options() {
        return get("options");
    }

    IndexBuilder fields(final List<Field> fields) {
        put("fields", fields.toArray(new Field[0]));
        return this;
    }

    IndexBuilder fields(final Field... fields) {
        put("fields", fields);
        return this;
    }

    /**
     * Options to apply to the index.  Use of this field will ignore any of the deprecated options defined on {@link Index} directly.
     */
    IndexBuilder options(final IndexOptions options) {
        put("options", options);
        return this;
    }

    /**
     * Create the index in the background
     */
    IndexBuilder background(final boolean background) {
        put("background", background);
        return this;
    }

    /**
     * The name of the index to create; default is to let the mongodb create a name (in the form of key1_1/-1_key2_1/-1...)
     */
    IndexBuilder name(final String name) {
        put("name", name);
        return this;
    }

    /**
     * Create the index with the sparse option
     */
    IndexBuilder sparse(final boolean sparse) {
        put("sparse", sparse);
        return this;
    }

    /**
     * Creates the index as a unique value index; inserting duplicates values in this field will cause errors
     */
    IndexBuilder unique(final boolean unique) {
        put("unique", unique);
        return this;
    }

    /**
     * List of fields (prepended with "-" for desc; defaults to asc).
     */
    IndexBuilder value(final String value) {
        put("value", value);
        return this;
    }
}
