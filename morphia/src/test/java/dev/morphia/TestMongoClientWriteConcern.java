/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */


package dev.morphia;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import org.junit.Test;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import static com.mongodb.WriteConcern.UNACKNOWLEDGED;
import static org.junit.Assert.assertEquals;

public class TestMongoClientWriteConcern extends TestBase {

    public TestMongoClientWriteConcern() {
        super(new MongoClient(new MongoClientURI(getMongoURI(),
                MongoClientOptions.builder().writeConcern(UNACKNOWLEDGED))));
    }

    @Test
    public void defaultWriteConcern() throws Exception {
        assertEquals(UNACKNOWLEDGED, getDs().getDefaultWriteConcern());
        getAds().insert(new SimpleEntity(1));
        getAds().insert(new SimpleEntity(1));
        assertEquals(1, getDs().getCount(SimpleEntity.class));
    }

    @Entity
    private static class SimpleEntity {

        @Id
        private int id;

        SimpleEntity(final int id) {
            this.id = id;
        }
    }
}
