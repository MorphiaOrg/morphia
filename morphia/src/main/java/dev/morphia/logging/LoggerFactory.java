/*
 * Copyright (c) 2008 - 2014 MongoDB, Inc. <http://mongodb.com>
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

package dev.morphia.logging;

/**
 * A generic logger factory interface used internally by Morphia.  At runtime the actual implementation used is chosen to match which
 * logging framework (e.g., java.util.logging vs slf4j) is used in the application.
 */
public interface LoggerFactory {
    /**
     * Gets or creates a Logger for the given class.
     *
     * @param c the class to use for naming
     * @return the Logger
     */
    Logger get(Class<?> c);
}
