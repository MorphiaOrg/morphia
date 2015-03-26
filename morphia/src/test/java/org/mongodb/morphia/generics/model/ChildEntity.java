package org.mongodb.morphia.generics.model;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "children")
public class ChildEntity extends FatherEntity<ChildEmbedded> {
    public ChildEntity() throws Exception {
        super();
    }

}
