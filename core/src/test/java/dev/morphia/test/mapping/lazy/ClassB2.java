package dev.morphia.test.mapping.lazy;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
public class ClassB2 implements InterfaceB {
    @Id
    ObjectId id;

    int age;

    ClassB2() {
    }

    public ClassB2(int age) {
        this.age = age;
    }

    @Override
    public ObjectId getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ClassB2 b2 = (ClassB2) o;

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
