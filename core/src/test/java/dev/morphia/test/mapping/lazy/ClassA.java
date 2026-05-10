package dev.morphia.test.mapping.lazy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;

import org.bson.types.ObjectId;

@Entity
public class ClassA implements InterfaceA {
    @Id
    ObjectId _id;
    @Reference(lazy = true)
    InterfaceB b;

    @Reference(lazy = true)
    InterfaceB reference;
    @Reference(lazy = true)
    List<InterfaceB> list;
    @Reference(lazy = true)
    Set<InterfaceB> set;
    @Reference(lazy = true)
    Map<String, InterfaceB> map;

    @Override
    public ObjectId getId() {
        return _id;
    }

    @Override
    public InterfaceB getB() {
        return b;
    }
}
