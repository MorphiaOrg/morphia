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

package dev.morphia.converters;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.query.FindOptions;
import dev.morphia.testutil.TestEntity;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TestStoringMapWithDateKey extends TestBase {

    private Locale locale;

    @After
    public void resetLocale() {
        Locale.setDefault(locale);
    }

    @Before
    public void setLocale() {
        locale = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);
    }

    @Test
    public void testSaveFindEntity() {
        getMorphia().map(User.class);
        final User expectedUser = new User();
        expectedUser.addValue(new Date(), 10d);

        getDs().save(expectedUser);
        Assert.assertNotNull(getDs().find(User.class).find(new FindOptions().limit(1)).tryNext());
    }
}

@Entity
class User extends TestEntity {
    private final Map<Date, Double> userMap = new HashMap<Date, Double>();

    public void addValue(final Date date, final Double value) {
        userMap.put(date, value);
    }
}
