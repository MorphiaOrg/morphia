package dev.morphia.mapping;

import java.time.ZoneId;

import static java.time.ZoneId.of;
import static java.time.ZoneId.systemDefault;

/**
 * This enum is used to determine how Java 8 dates and times are stored in the database.
 *
 */
@SuppressWarnings("Since15")
public enum DateForm {
    UTC {
        @Override
        public ZoneId getZone() {
            return of("UTC");
        }
    },
    /**
     * @deprecated This will be removed in 2.0.  It is intended to bridge the gap when correcting the storage of data/time values in the
     * database.  {@link #UTC} should be used and will be the default in 2.0.  In 1.5 it is {@link #SYSTEM_DEFAULT} for backwards
     * compatibility.
     */
    SYSTEM_DEFAULT {
        @Override
        public ZoneId getZone() {
            return systemDefault();
        }
    };

    public abstract ZoneId getZone();
}
