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

package org.mongodb.morphia.mapping.lazy;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MapperOptions;

/**
 * Defines a DatastoreProvider that clones the Mapper instance and binds the new Mapper and a new DatastoreProvider directly
 */
public class CloningDatastoreProvider implements DatastoreProvider {

    @Override
    public Datastore get() {
        throw new UnsupportedOperationException("get() should never be called on CloningDatastoreProvider.");
    }

    @Override
    public void register(final Datastore ds) {
        final Mapper original = ((DatastoreImpl) ds).getMapper();
        final MapperOptions options = new MapperOptions(original.getOptions());
        final SingleDatastoreProvider provider = new SingleDatastoreProvider();
        provider.register(ds);
        options.setDatastoreProvider(provider);
        ((DatastoreImpl) ds).setMapper(new Mapper(options, original));
    }
}
