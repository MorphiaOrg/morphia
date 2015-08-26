/*
 * Copyright (c) 2008-2015 MongoDB, Inc.
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

package org.mongodb.morphia.mapping;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.HashMap;
import java.util.Map;

public class EmbeddedMappingTest extends TestBase {
    @Test
    public void mapGenericEmbeds() {
        getMorphia().map(AuditEntry.class, Delta.class);

        final AuditEntry<String> entry = new AuditEntry<String>();

        final HashMap<String, Object> before = new HashMap<String, Object>();
        final HashMap<String, Object> after = new HashMap<String, Object>();
        before.put("before", 42);
        after.put("after", 84);

        entry.delta = new Delta<String>(before, after);
        getDs().save(entry);

        final AuditEntry fetched = getDs().createQuery(AuditEntry.class)
                                          .filter("id = ", entry.id)
                                          .get();

        Assert.assertEquals(entry, fetched);
    }

    @Entity(value = "audit", noClassnameStored = true)
    public static class AuditEntry<T> {
        @Id
        private ObjectId id;

        @Embedded
        private Delta<T> delta;

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + delta.hashCode();
            return result;
        }        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final AuditEntry<?> that = (AuditEntry<?>) o;

            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }
            return delta.equals(that.delta);

        }


    }

    @Embedded
    public static class Delta<T> {
        private Map<String, Object> before;
        private Map<String, Object> after;

        private Delta() {
        }

        public Delta(final Map<String, Object> before, final Map<String, Object> after) {
            this.before = before;
            this.after = after;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Delta<?> delta = (Delta<?>) o;

            if (!before.equals(delta.before)) {
                return false;
            }
            return after.equals(delta.after);

        }

        @Override
        public int hashCode() {
            int result = before.hashCode();
            result = 31 * result + after.hashCode();
            return result;
        }
    }
}
