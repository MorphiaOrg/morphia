package xyz.morphia.generics.model;

import xyz.morphia.annotations.Entity;

@Entity(value = "children")
public class ChildEntity extends FatherEntity<ChildEmbedded> {
    public ChildEntity() throws Exception {
        super();
    }

}
