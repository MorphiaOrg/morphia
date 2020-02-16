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

package dev.morphia.legacy;

import dev.morphia.TestDatastore.FacebookUser;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.legacy.LegacyTestBase;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class TestLegacyDatastore extends LegacyTestBase {
    @Test
    public void testFindAndModifyLegacy() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        UpdateOperations<FacebookUser> modify = getDs().createUpdateOperations(FacebookUser.class).inc("loginCount");

        FacebookUser results = getDs().findAndModify(query, modify);

        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 2)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, results.loginCount);
    }
}
