/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
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

package org.mongodb.morphia.testmodel;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;

import java.io.Serializable;

@Embedded
public class CompoundId implements Serializable {
    private ObjectId value;
    private String name;

    CompoundId() {
    }

    public CompoundId(final String n) {
        name = n;
        value = new ObjectId();
    }

    public ObjectId getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof CompoundId)) {
            return false;
        }
        final CompoundId other = ((CompoundId) obj);
        return other.value.equals(value) && other.name.equals(name);
    }

}
