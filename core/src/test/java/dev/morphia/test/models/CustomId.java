package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import org.bson.types.ObjectId;

@Entity
public class CustomId {

    @Property("v")
    private ObjectId id;
    @Property("t")
    private String type;

    public CustomId() {
    }

    public CustomId(String type) {
        this.id = new ObjectId();
        this.type = type;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CustomId)) {
            return false;
        }
        final CustomId other = (CustomId) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (type == null) {
            return other.type == null;
        } else {
            return type.equals(other.type);
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CustomId [");
        if (id != null) {
            builder.append("id=").append(id).append(", ");
        }
        if (type != null) {
            builder.append("type=").append(type);
        }
        builder.append("]");
        return builder.toString();
    }
}
