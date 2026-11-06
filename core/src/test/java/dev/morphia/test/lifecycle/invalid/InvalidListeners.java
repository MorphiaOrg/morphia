package dev.morphia.test.lifecycle.invalid;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.PostLoadListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;

import org.bson.types.ObjectId;

/**
 * Entities whose {@code @EntityListeners} classes fail to map. They're kept out of {@code dev.morphia.test.lifecycle}, which
 * other tests scan.
 */
public final class InvalidListeners {
    private InvalidListeners() {
    }

    @Entity("invalid_listeners_annotated")
    @EntityListeners(Annotated.class)
    public static class AnnotatedListened {
        @Id
        ObjectId id;
    }

    @Entity("invalid_listeners_marker")
    @EntityListeners(MarkerOnly.class)
    public static class MarkerListened {
        @Id
        ObjectId id;
    }

    /**
     * Has an annotated method, which only an entity may have, as well as an interface's.
     */
    public static class Annotated implements PostLoadListener<AnnotatedListened> {
        @PrePersist
        void annotated(AnnotatedListened entity) {
        }

        @Override
        public void postLoad(AnnotatedListened entity, Datastore datastore) {
        }
    }

    public static class MarkerOnly implements EntityListener<Object> {
    }

    @Entity("invalid_listeners_failing")
    @EntityListeners(Failing.class)
    public static class FailingListened {
        @Id
        ObjectId id;
    }

    public static class Failing implements PostLoadListener<FailingListened> {
        public Failing() {
            throw new IllegalStateException("no listener today");
        }

        @Override
        public void postLoad(FailingListened entity, Datastore datastore) {
        }
    }
}
