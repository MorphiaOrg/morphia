package org.mongodb.morphia.issueB;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity(value = "childs", noClassnameStored = true)
public class Child {
    @Id
    private ObjectId id;
}
