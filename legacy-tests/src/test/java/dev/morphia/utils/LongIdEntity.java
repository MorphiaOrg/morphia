package dev.morphia.utils;


import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Transient;
import dev.morphia.query.Query;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.updates.UpdateOperators.inc;


@Entity
public abstract class LongIdEntity {
    @Id
    private Long myLongId;
    @Transient
    private final Datastore ds;

    protected LongIdEntity(Datastore ds) {
        this.ds = ds;
    }

    public Datastore getDs() {
        return ds;
    }

    public Long getMyLongId() {
        return myLongId;
    }

    @Entity(value = "ids", useDiscriminator = false)
    public static class StoredId {
        @Id
        private final String className;
        private final Long value = 1L;

        public StoredId(String name) {
            className = name;
        }

        protected StoredId() {
            className = "";
        }

        public String getClassName() {
            return className;
        }

        public Long getValue() {
            return value;
        }
    }

    @PrePersist
    void prePersist() {
        if (myLongId == null) {
            final String collName = ds.getMapper().getCollection(getClass()).getNamespace().getCollectionName();
            final Query<StoredId> q = ds.find(StoredId.class).filter(eq("_id", collName));
            StoredId newId = q.modify(inc("value")).execute();
            if (newId == null) {
                newId = new StoredId(collName);
                ds.save(newId);
            }

            myLongId = newId.getValue();
        }
    }
}
