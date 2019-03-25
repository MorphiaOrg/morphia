package dev.morphia.mapping.lazy;

import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

public class B2 implements InterfaceB {
    @Id
    ObjectId id;

    int age;

    B2() {
    }

    public B2(final int age) {
        this.age = age;
    }

    @Override
    public ObjectId getId() {
        return id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final B2 b2 = (B2) o;

        if (age != b2.age) {
            return false;
        }
        return id != null ? id.equals(b2.id) : b2.id == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + age;
        return result;
    }

    @Override
    public String toString() {
        return "B2{" +
               "id=" + id +
               ", age=" + age +
               '}';
    }
}
