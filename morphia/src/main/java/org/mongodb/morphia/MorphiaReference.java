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

package org.mongodb.morphia;

import com.mongodb.DBRef;
import org.mongodb.morphia.annotations.Reference;

/**
 * Defines a reference to a mapped entity.  This class is the replacement for annotating referenced entities with {@link Reference} and
 * replaces implicit fetches with explicit queries against the database.
 *
 * @param <T> the type of the entity referenced
 */
public final class MorphiaReference<T> {
    private DBRef dbRef;
    private boolean idOnly = false;

    private transient T entity;
    private transient Class<?> typeClass;

    public MorphiaReference(final Object id, final String collection, final T entity) {
        dbRef = new DBRef(collection, id);
        this.entity = entity;
    }

    public boolean isIdOnly() {
        return idOnly;
    }

    public MorphiaReference<T> idOnly(final boolean idOnly) {
        this.idOnly = idOnly;
        return this;
    }

    /**
     * Returns the collection this reference is stored in.  This may differ from the mapped collection name.
     *
     * @return the collection name
     */
    public String getCollection() {
        return dbRef.getCollectionName();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MorphiaReference)) {
            return false;
        }

        final MorphiaReference<?> that = (MorphiaReference<?>) o;

        if (idOnly != that.idOnly) {
            return false;
        }
        if (dbRef != null ? !dbRef.equals(that.dbRef) : that.dbRef != null) {
            return false;
        }
        return typeClass != null ? typeClass.equals(that.typeClass) : that.typeClass == null;

    }

    @Override
    public int hashCode() {
        int result = dbRef != null ? dbRef.hashCode() : 0;
        result = 31 * result + (idOnly ? 1 : 0);
        result = 31 * result + (typeClass != null ? typeClass.hashCode() : 0);
        return result;
    }

    public DBRef getDBRef() {
        return dbRef;
    }

    public T getEntity() {
        return entity;
    }

    void setEntity(final T entity) {
        this.entity = entity;
    }

}
