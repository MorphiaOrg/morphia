package dev.morphia.test.mapping.lazy;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
public class ClassB1 implements InterfaceB {
    @Id
    ObjectId id;

    String name;

    ClassB1() {
    }

    public ClassB1(String name) {
        this.name = name;
    }

    @Override
    public ObjectId getId() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ClassB1 b1 = (ClassB1) o;

        if (id != null ? !id.equals(b1.id) : b1.id != null) {
            return false;
        }
        return name.equals(b1.name);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "B1{" +
               "id=" + id +
               ", name='" + name + '\'' +
               '}';
    }
}
