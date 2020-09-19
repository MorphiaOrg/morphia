/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package dev.morphia.mapping;

/**
 * An exception indicating an error mapping a type
 */
public class MappingException extends RuntimeException {
    /**
     * Creates an exception with a message
     *
     * @param message the message to record
     */
    public MappingException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and a cause
     *
     * @param message the message to record
     * @param cause   the
     */
    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
