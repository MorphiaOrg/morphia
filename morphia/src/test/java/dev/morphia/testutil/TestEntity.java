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


package dev.morphia.testutil;


import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.io.Serializable;


/**
 * Example for a base class to all entities.
 *
 * @author Olafur Gauti Gudmundsson
 */

public class TestEntity implements Serializable {
    @Id
    public ObjectId id = new ObjectId();

    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId id) {
        this.id = id;
    }
}
