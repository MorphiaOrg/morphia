package xyz.morphia.utils;


import xyz.morphia.Datastore;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.PrePersist;
import xyz.morphia.annotations.Transient;
import xyz.morphia.query.Query;
import xyz.morphia.query.UpdateOperations;


public abstract class LongIdEntity {
    @Transient
    private final Datastore ds;
    @Id
    private Long myLongId;

    protected LongIdEntity(final Datastore ds) {
        this.ds = ds;
    }

    public Datastore getDs() {
        return ds;
    }

    public Long getMyLongId() {
        return myLongId;
    }

    /**
     * Used to store counters for other entities.
     *
     * @author skot
     */

    @Entity(value = "ids", noClassnameStored = true)
    public static class StoredId {
        @Id
        private final String className;
        private final Long value = 1L;

        public StoredId(final String name) {
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
            final String collName = ds.getCollection(getClass()).getName();
            final Query<StoredId> q = ds.find(StoredId.class).filter("_id", collName);
            final UpdateOperations<StoredId> uOps = ds.createUpdateOperations(StoredId.class).inc("value");
            StoredId newId = ds.findAndModify(q, uOps);
            if (newId == null) {
                newId = new StoredId(collName);
                ds.save(newId);
            }

            myLongId = newId.getValue();
        }
    }
}
