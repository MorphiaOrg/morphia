package org.mongodb.morphia.generics;

import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.generics.model.AnotherChildEmbedded;
import org.mongodb.morphia.generics.model.ChildEmbedded;
import org.mongodb.morphia.generics.model.ChildEntity;

import static java.util.Arrays.asList;

public class WildcardsTest extends TestBase {

    @Test
    public void example() throws Exception {
        ChildEntity entity = new ChildEntity();
        entity.setEmbeddedList(asList(new ChildEmbedded("first"), new ChildEmbedded("second"), new AnotherChildEmbedded("third")));
        getDs().save(entity);

        ChildEntity childEntity = getDs().find(ChildEntity.class).get();

        Assert.assertEquals(entity, childEntity);
    }
}
