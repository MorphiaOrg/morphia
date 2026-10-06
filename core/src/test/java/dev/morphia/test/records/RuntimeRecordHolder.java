package dev.morphia.test.records;

import java.util.List;
import java.util.Objects;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * A regular class entity that embeds a record, outside the packages listed in {@code morphia.packages}.
 */
@Entity("runtime_record_holders")
public class RuntimeRecordHolder {
    @Id
    private ObjectId id;
    private RuntimeRecordAddress address;
    private List<RuntimeRecordAddress> history;

    public RuntimeRecordHolder() {
    }

    public RuntimeRecordHolder(RuntimeRecordAddress address, List<RuntimeRecordAddress> history) {
        this.address = address;
        this.history = history;
    }

    public ObjectId getId() {
        return id;
    }

    public RuntimeRecordAddress getAddress() {
        return address;
    }

    public List<RuntimeRecordAddress> getHistory() {
        return history;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RuntimeRecordHolder that)) {
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
        return "RuntimeRecordHolder{id=" + id + ", address=" + address + ", history=" + history + "}";
    }
}
