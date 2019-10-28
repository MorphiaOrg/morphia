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

package dev.morphia.mapping;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.DateStorage;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.internal.MorphiaCursor;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import static java.time.temporal.ChronoUnit.DAYS;

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

    @Test
    public void dateForm() {
        LocalDate localDate = LocalDate.of(1995, 10, 15);
        LocalDateTime localDateTime = LocalDateTime.of(2016, 4, 10, 2, 15, 16, 123 * 1000000);

        final MongoCollection<Java8Entity> collection = getDs().getCollection(Java8Entity.class);

        Java8Entity created = createEntity(getDs(), null, localDate, localDateTime, null);
        final Java8Entity loaded = getDs().createQuery(Java8Entity.class).first();

        final Java8Entity loaded3 = getDs().createQuery(Java8Entity.class).first();

        Java8Entity created2 = createEntity(getDs(), null, localDate, localDateTime, null);
        final Java8Entity loaded2 = getDs().createQuery(Java8Entity.class).first();

        Assert.assertNotEquals(created, created2);
        Assert.assertEquals(loaded.getLocalDate(), loaded2.getLocalDate());
        Assert.assertEquals(loaded.getLocalDateTime(), loaded2.getLocalDateTime());

        Assert.assertEquals(loaded.getLocalDate(), loaded3.getLocalDate());
    }

    private void compare(final Datastore datastore, final Java8Entity entity, final String field, final Object value) {
        Query<Java8Entity> query = datastore.find(Java8Entity.class)
                                            .field(field).equal(value);
        Java8Entity actual = query.execute(new FindOptions().logQuery()
                                                            .limit(1))
                                      .tryNext();
        Assert.assertEquals(query.getLoggedQuery(), entity, actual);
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

    @SuppressWarnings("Since15")
    @Entity("java8")
    public static class Java8Entity {
        @Id
        private ObjectId id;
        private Instant instant;
        private LocalDate localDate;
        private LocalDateTime localDateTime;
        private LocalTime localTime;

        public Java8Entity() {
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public Instant getInstant() {
            return instant;
        }

        public void setInstant(final Instant instant) {
            this.instant = instant;
        }

        public LocalDate getLocalDate() {
            return localDate;
        }

        public void setLocalDate(final LocalDate localDate) {
            this.localDate = localDate;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public void setLocalDateTime(final LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
        }

        public LocalTime getLocalTime() {
            return localTime;
        }

        public void setLocalTime(final LocalTime localTime) {
            this.localTime = localTime;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Java8Entity)) {
                return false;
            }

            final Java8Entity that = (Java8Entity) o;

            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }
            if (instant != null ? !instant.equals(that.instant) : that.instant != null) {
                return false;
            }
            if (localDate != null ? !localDate.equals(that.localDate) : that.localDate != null) {
                return false;
            }
            if (localDateTime != null ? !localDateTime.equals(that.localDateTime) : that.localDateTime != null) {
                return false;
            }
            return localTime != null ? localTime.equals(that.localTime) : that.localTime == null;

        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (instant != null ? instant.hashCode() : 0);
            result = 31 * result + (localDate != null ? localDate.hashCode() : 0);
            result = 31 * result + (localDateTime != null ? localDateTime.hashCode() : 0);
            result = 31 * result + (localTime != null ? localTime.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return String.format("Java8Entity{id=%s, instant=%s, localDate=%s, localDateTime=%s, localTime=%s}",
                id, instant, localDate, localDateTime, localTime);
        }
    }
}
