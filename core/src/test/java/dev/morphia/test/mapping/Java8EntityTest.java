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

package dev.morphia.test.mapping;

import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.lte;
import static dev.morphia.query.experimental.filters.Filters.ne;
import static java.time.temporal.ChronoUnit.DAYS;

public class Java8EntityTest extends TestBase {

    @Test
    public void dateForm() {
        LocalDate localDate = LocalDate.of(1995, 10, 15);
        LocalDateTime localDateTime = LocalDateTime.of(2016, 4, 10, 2, 15, 16, 123 * 1000000);

        Java8Entity created = createEntity(getDs(), null, localDate, localDateTime, null);
        final Java8Entity loaded = getDs().find(Java8Entity.class).first();

        final Java8Entity loaded3 = getDs().find(Java8Entity.class).first();

        Java8Entity created2 = createEntity(getDs(), null, localDate, localDateTime, null);
        final Java8Entity loaded2 = getDs().find(Java8Entity.class).first();

        Assert.assertNotEquals(created, created2);
        Assert.assertEquals(loaded.getLocalDate(), loaded2.getLocalDate());
        Assert.assertEquals(loaded.getLocalDateTime(), loaded2.getLocalDateTime());

        Assert.assertEquals(loaded.getLocalDate(), loaded3.getLocalDate());
    }

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
        Assert.assertEquals(getDs().find(Java8Entity.class).filter(lte("instant", instant.plus(1, DAYS))).count(), 2L);
        Assert.assertEquals(getDs().find(Java8Entity.class).filter(eq("localDate", localDate.plus(1, DAYS))).count(), 1L);
        Assert.assertEquals(getDs().find(Java8Entity.class).filter(eq("localDate", localDate.minus(1, DAYS))).count(), 0L);
        Assert.assertEquals(getDs().find(Java8Entity.class).filter(ne("localDateTime", localDateTime.plus(6, DAYS))).count(), 9L);
    }

    private void compare(Datastore datastore, Java8Entity entity, String field, Object value) {
        Query<Java8Entity> query = datastore.find(Java8Entity.class)
                                            .filter(eq(field, value));
        FindOptions options = new FindOptions().logQuery()
                                               .limit(1);
        Java8Entity actual = query.iterator(options)
                                  .tryNext();
        Assert.assertEquals(actual, entity, getDs().getLoggedQuery(options));
    }

    private Java8Entity createEntity(Datastore ds, Instant instant, LocalDate localDate,
                                     LocalDateTime localDateTime, LocalTime localTime) {
        Java8Entity entity = new Java8Entity();
        entity.setInstant(instant);
        entity.setLocalDate(localDate);
        entity.setLocalDateTime(localDateTime);
        entity.setLocalTime(localTime);
        ds.save(entity);
        return entity;
    }

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

        public void setId(ObjectId id) {
            this.id = id;
        }

        public Instant getInstant() {
            return instant;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }

        public LocalDate getLocalDate() {
            return localDate;
        }

        public void setLocalDate(LocalDate localDate) {
            this.localDate = localDate;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public void setLocalDateTime(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
        }

        public LocalTime getLocalTime() {
            return localTime;
        }

        public void setLocalTime(LocalTime localTime) {
            this.localTime = localTime;
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
        public boolean equals(Object o) {
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
        public String toString() {
            return String.format("Java8Entity{id=%s, instant=%s, localDate=%s, localDateTime=%s, localTime=%s}",
                id, instant, localDate, localDateTime, localTime);
        }
    }
}
