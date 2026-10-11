package dev.morphia.test.models.records;

import java.util.List;
import java.util.Objects;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * A regular class entity that embeds a record, in a package listed in {@code morphia.packages}.
 */
@Entity("aot_record_holders")
public class AotRecordHolder {
    @Id
    private ObjectId id;
    private AotRecordAddress address;
    private List<AotRecordAddress> history;

    public AotRecordHolder() {
    }

    public AotRecordHolder(AotRecordAddress address, List<AotRecordAddress> history) {
        this.address = address;
        this.history = history;
    }

    public ObjectId getId() {
        return id;
    }

    public AotRecordAddress getAddress() {
        return address;
    }

    public List<AotRecordAddress> getHistory() {
        return history;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AotRecordHolder that)) {
            return false;
        }
        return Objects.equals(id, that.id) && Objects.equals(address, that.address) && Objects.equals(history, that.history);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, address, history);
    }

    @Override
    public String toString() {
        return "AotRecordHolder{id=" + id + ", address=" + address + ", history=" + history + "}";
    }
}
