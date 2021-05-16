package dev.morphia.test.models.generics;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.List;

@Entity
public abstract class FatherEntity<T extends EmbeddedType> {

    @Id
    private final ObjectId id = new ObjectId();
    private List<? extends EmbeddedType> embeddedList;
    private Another embedded;

    public FatherEntity() {
    }

    public ObjectId getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FatherEntity)) {
            return false;
        }

        final FatherEntity<?> that = (FatherEntity<?>) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        return embeddedList != null ? embeddedList.equals(that.embeddedList) : that.embeddedList == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (embeddedList != null ? embeddedList.hashCode() : 0);
        return result;
    }

    public void setEmbeddedList(List<? extends EmbeddedType> embeddedList) {
        this.embeddedList = embeddedList;
    }
}
