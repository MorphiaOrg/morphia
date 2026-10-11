package dev.morphia.mapping;

import java.time.ZoneId;

import static java.time.ZoneId.systemDefault;

/**
 * This enum is used to determine how JSR 310 dates and times are stored in the database.
 *
 * @since 2.0
 */
public enum DateStorage {
    /**
     * the UTC format
     */
    UTC {
        @Override
        public ZoneId getZone() {
            return Zones.UTC;
        }
    },

    /**
     * the system default format
     */
    SYSTEM_DEFAULT {
        @Override
        public ZoneId getZone() {
            return systemDefault();
        }
    };

    /**
     * @return the ZoneId for this storage type
     */
    public abstract ZoneId getZone();

    /**
     * Parsing "UTC" builds a new {@code ZoneId} every time, and the date codecs ask for the zone on every value.
     */
    private static final class Zones {
        private static final ZoneId UTC = ZoneId.of("UTC");
    }
}
