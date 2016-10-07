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

import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.utils.IndexType;

class FieldBuilder extends AnnotationBuilder<Field> {
    @Override
    Class<Field> getAnnotationType() {
        return Field.class;
    }

    FieldBuilder type(final IndexType type) {
        put("type", type);
        return this;
    }

    FieldBuilder value(final String value) {
        put("value", value);
        return this;
    }

    FieldBuilder weight(final int weight) {
        put("weight", weight);
        return this;
    }

}
