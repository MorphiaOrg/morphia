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


package org.mongodb.morphia.testmodel;


import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Olafur Gauti Gudmundsson
 */
@Entity("stuff")
public class RecursiveParent extends TestEntity {
    @Reference
    private RecursiveChild child;

    public RecursiveChild getChild() {
        return child;
    }

    public void setChild(final RecursiveChild child) {
        this.child = child;
    }
}
