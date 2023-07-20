package dev.morphia.test.mapping.codec.pojo.generics;

import java.util.List;
import java.util.Locale;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
public class Subtypes {
    @Id
    public String id;
    public PartialHashMap<Integer> partialHashMap;
    public FullHashMap fullHashMap;
    public List<Locale> genericList;
    public PartialList partialList;
    public String name;
    public int age;
}
