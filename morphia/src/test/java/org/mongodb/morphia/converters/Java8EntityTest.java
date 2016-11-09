/*
 * Copyright 2016 MongoDB, Inc.
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

package org.mongodb.morphia.converters;

import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.TestBase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static java.time.temporal.ChronoUnit.DAYS;

@SuppressWarnings("Since15")
public class Java8EntityTest extends TestBase {

    @Test
    public void queries() {
        Instant instant = Instant.ofEpochMilli(System.currentTimeMillis());
        LocalDate localDate = LocalDate.of(1995, 10, 15);
        LocalDateTime localDateTime = LocalDateTime.of(2016, 4, 10, 14, 15, 16, 123 * 1000000);
        LocalTime localTime = LocalTime.of(10, 29, 15, 848000000);

        Java8Entity entity = createEntity(getDs(), instant, localDate, localDateTime, localTime);

        compare(getDs(), entity, "instant", instant);
        compare(getDs(), entity, "localDate", localDate);
        compare(getDs(), entity, "localDateTime", localDateTime);
        compare(getDs(), entity, "localTime", localTime);
    }

    @Test
    public void rangeQueries() {
        Instant instant = Instant.ofEpochMilli(System.currentTimeMillis());
        LocalDate localDate = LocalDate.of(1995, 10, 15);
        LocalDateTime localDateTime = LocalDateTime.of(2016, 4, 10, 14, 15, 16, 123 * 1000000);
        LocalTime localTime = LocalTime.of(10, 29, 15, 848493);

        for (int i = 0; i < 10; i++) {
            createEntity(getDs(),
                         instant.plus(i, DAYS),
                         localDate.plus(i, DAYS),
                         localDateTime.plus(i, DAYS),
                         localTime.plus(i, ChronoUnit.HOURS));
        }
        Assert.assertEquals(2L, getDs().find(Java8Entity.class).field("instant").lessThanOrEq(instant.plus(1, DAYS)).count());
        Assert.assertEquals(1L, getDs().find(Java8Entity.class).field("localDate").equal(localDate.plus(1, DAYS)).count());
        Assert.assertEquals(0L, getDs().find(Java8Entity.class).field("localDate").equal(localDate.minus(1, DAYS)).count());
        Assert.assertEquals(9L, getDs().find(Java8Entity.class).field("localDateTime")
                                       .notEqual(localDateTime.plus(6, DAYS)).count());
    }

    private void compare(final Datastore datastore, final Java8Entity entity, final String field, final Object value) {
        Java8Entity actual = datastore.find(Java8Entity.class).field(field).equal(value).get();
        Assert.assertEquals(entity, actual);
    }

    private Java8Entity createEntity(final Datastore ds, final Instant instant, final LocalDate localDate,
                                     final LocalDateTime localDateTime, final LocalTime localTime) {
        Java8Entity entity = new Java8Entity();
        entity.setInstant(instant);
        entity.setLocalDate(localDate);
        entity.setLocalDateTime(localDateTime);
        entity.setLocalTime(localTime);
        ds.save(entity);
        return entity;
    }

}
