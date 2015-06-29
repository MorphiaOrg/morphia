package org.mongodb.morphia.issueB;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

@Entity(value = "parents", noClassnameStored = true)
public class Parent {
    @Id
    private ObjectId id;

    @Reference(lazy = true)
    private List<Child> childs = new ArrayList<Child>();

    public List<Child> getChilds() {
        return childs;
    }
}
