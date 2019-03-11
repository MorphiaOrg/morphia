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


package dev.morphia.testdaos;


import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import dev.morphia.Morphia;
import dev.morphia.dao.BasicDAO;
import dev.morphia.testmodel.Hotel;


/**
 * @author Olafur Gauti Gudmundsson
 */
@Deprecated
public class HotelDAO extends BasicDAO<Hotel, ObjectId> {

    public HotelDAO(final Morphia morphia, final MongoClient mongoClient) {
        super(mongoClient, morphia, "morphia_test");
    }
}
