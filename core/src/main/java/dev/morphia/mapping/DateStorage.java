package dev.morphia.mapping;

import dev.morphia.DatastoreImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;

import static java.time.ZoneId.of;
import static java.time.ZoneId.systemDefault;

/**
 * This enum is used to determine how Java 8 dates and times are stored in the database.
 *
 * @since 2.0
 */
public enum DateStorage {
    UTC {
        @Override
        public ZoneId getZone() {
            return of("UTC");
        }
    },

    SYSTEM_DEFAULT {
        @Override
        public ZoneId getZone() {
            if (!warningLogged) {
                warningLogged = true;
                LOG.warn("Currently using the system default zoneId for encoding.  This default will change in 2.0 to use UTC which will "
                         + "likely break your application.  Consult the migration guide for mitigation suggestions.");
            }

            return systemDefault();
        }
    };

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreImpl.class);
    private static boolean warningLogged;

    /**
     * @return the ZoneId for this storage type
     */
    public abstract ZoneId getZone();
}
