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
import dev.morphia.annotations.Text;

class TextBuilder extends AnnotationBuilder<Text> implements Text {
    @Override
    public Class<Text> annotationType() {
        return Text.class;
    }

    @Override
    public IndexOptions options() {
        return get("options");
    }

    @Override
    public int value() {
        return get("value");
    }

    TextBuilder options(final IndexOptions options) {
        put("options", options);
        return this;
    }

    TextBuilder value(final int value) {
        put("value", value);
        return this;
    }

}
