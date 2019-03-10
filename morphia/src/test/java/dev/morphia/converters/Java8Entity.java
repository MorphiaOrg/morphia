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

package dev.morphia.converters;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@SuppressWarnings("Since15")
@Entity("java8")
public class Java8Entity {
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
