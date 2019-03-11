package dev.morphia.generics.model;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Id;

import java.util.List;

public abstract class FatherEntity<T extends FatherEmbedded> {

    @Id
    private ObjectId id = new ObjectId();

    private List<? extends FatherEmbedded> embeddedList;

    public FatherEntity() throws Exception {
        super();
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + embeddedList.hashCode();
        return result;
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

        return id.equals(that.id) && embeddedList.equals(that.embeddedList);

    }

    public void setEmbeddedList(final List<? extends FatherEmbedded> embeddedList) {
        this.embeddedList = embeddedList;
    }
}
