/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia;

import com.mongodb.client.model.ReturnDocument;

/**
 * The options for find and modify operations.
 *
 * @since 1.3
 * @deprecated use {@link ModifyOptions} instead
 */
@Deprecated(since = "2.0", forRemoval = true)
public final class FindAndModifyOptions extends ModifyOptions {
    /**
     * Sets the returnNew
     *
     * @param returnNew the returnNew
     * @return this
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public FindAndModifyOptions returnNew(boolean returnNew) {
        returnDocument(returnNew ? ReturnDocument.AFTER : ReturnDocument.BEFORE);
        return this;
    }

}
