package org.mongodb.morphia.issueB;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.mapping.Mapper;

public class TestRefMorphia extends TestBase {

    @Test
    public void test01() throws Exception {
        Child child1 = new Child();
        getDs().save(child1);

        Parent parent1 = new Parent();
        parent1.getChilds().add(child1);
        getDs().save(parent1);

        List<Parent> parentList = getDs().find(Parent.class).asList();
        Assert.assertEquals(1, parentList.size());

        // reset Datastore to reset internal Mapper cache, so Child class
        // already cached by previou save is cleared
        Datastore localDs = getMorphia().createDatastore(getMongoClient(), new Mapper(), getDb().getName());

        parentList = localDs.find(Parent.class).asList();
        Assert.assertEquals(1, parentList.size());
    }

}
