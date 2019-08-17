package dev.morphia.generics.model;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Id;

import java.util.List;

public abstract class FatherEntity<T extends EmbeddedType> {

    @Id
    private ObjectId id = new ObjectId();
    private List<? extends EmbeddedType> embeddedList;

    public FatherEntity() {
    }

    @Override
    public boolean equals(final Object o) {
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

    public void setEmbeddedList(final List<? extends EmbeddedType> embeddedList) {
        this.embeddedList = embeddedList;
    }
}
