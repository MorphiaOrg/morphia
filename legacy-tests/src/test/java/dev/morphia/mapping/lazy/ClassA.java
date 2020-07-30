package dev.morphia.mapping.lazy;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.experimental.MorphiaReference;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
public class ClassA implements InterfaceA {
    @Id
    ObjectId _id;
    @Reference(lazy = true)
    InterfaceB b;

    MorphiaReference<InterfaceB> reference;
    MorphiaReference<List<InterfaceB>> list;
    MorphiaReference<Set<InterfaceB>> set;
    MorphiaReference<Map<String, InterfaceB>> map;

    @Override
    public ObjectId getId() {
        return _id;
    }

    @Override
    public InterfaceB getB() {
        return b;
    }
}
