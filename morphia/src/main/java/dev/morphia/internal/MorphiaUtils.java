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

package dev.morphia.internal;

import java.util.List;

/**
 * This class provides a set of utilities for use in Morphia.  All these methods should be considered internal only and should not be
 * used in application code.
 *
 * @since 1.3
 */
public final class MorphiaUtils {
    private MorphiaUtils() {
    }

    /**
     * Joins strings with the given delimiter
     *
     * @param strings   the strings to join
     * @param delimiter the delimiter
     * @return the joined string
     */
    public static String join(final List<String> strings, final char delimiter) {
        StringBuilder builder = new StringBuilder();
        for (String element : strings) {
            if (builder.length() != 0) {
                builder.append(delimiter);
            }
            builder.append(element);
        }
        return builder.toString();
    }
}
