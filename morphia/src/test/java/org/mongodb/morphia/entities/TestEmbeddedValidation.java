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

package org.mongodb.morphia.entities;

import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.query.Query;

public class TestEmbeddedValidation extends TestBase {

    @Test
    public void testDottedNames() {
        ParentType parentType = new ParentType();
        EmbeddedSubtype embedded = new EmbeddedSubtype();
        embedded.setText("text");
        embedded.setNumber(42L);
        embedded.setFlag(true);
        parentType.setEmbedded(embedded);

        Datastore ds = getDs();
        ds.save(parentType);

        Query<ParentType> query = ds.createQuery(ParentType.class)
                                    .disableValidation()
                                    .field("embedded.flag").equal(true);

        Assert.assertEquals(parentType, query.get());
    }
}
