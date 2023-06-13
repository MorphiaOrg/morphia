package dev.morphia.test.models.external;

import java.util.Objects;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.models.errors.unannotated.external.ThirdPartyEmbedded;

import org.bson.types.ObjectId;

@Entity
public class HoldsUnannotated {
    public ThirdPartyEmbedded embedded;
    @Id
    private ObjectId id;

    @Override
    public int hashCode() {
        return Objects.hash(id, embedded);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HoldsUnannotated)) {
            return false;
        }
        HoldsUnannotated that = (HoldsUnannotated) o;
        return Objects.equals(id, that.id) && Objects.equals(embedded, that.embedded);
    }
}
