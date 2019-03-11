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

import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import dev.morphia.annotations.Validation;

/**
 * This is an internal class subject to change and removal.  Do not use.
 */
public class ValidationBuilder extends AnnotationBuilder<Validation> implements Validation {
    /**
     * @param action Do not use.
     * @return Do not use.
     */
    public ValidationBuilder action(final ValidationAction action) {
        put("action", action);
        return this;
    }

    @Override
    public Class<Validation> annotationType() {
        return Validation.class;
    }

    /**
     * @param level Do not use.
     * @return Do not use.
     */
    public ValidationBuilder level(final ValidationLevel level) {
        put("level", level);
        return this;
    }

    @Override
    public String value() {
        return get("value");
    }

    @Override
    public ValidationLevel level() {
        return get("level");
    }

    @Override
    public ValidationAction action() {
        return get("action");
    }

    /**
     * @param value Do not use.
     * @return Do not use.
     */
    public ValidationBuilder value(final String value) {
        put("value", value);
        return this;
    }
}
