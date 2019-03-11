package dev.morphia.generics;

import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.generics.model.AnotherChildEmbedded;
import dev.morphia.generics.model.ChildEmbedded;
import dev.morphia.generics.model.ChildEntity;
import dev.morphia.query.FindOptions;

import static java.util.Arrays.asList;

public class WildcardsTest extends TestBase {

    @Test
    public void example() throws Exception {
        ChildEntity entity = new ChildEntity();
        entity.setEmbeddedList(asList(new ChildEmbedded("first"), new ChildEmbedded("second"), new AnotherChildEmbedded("third")));
        getDs().save(entity);

        ChildEntity childEntity = getDs().find(ChildEntity.class)
                                         .find(new FindOptions().limit(1))
                                         .next();

        Assert.assertEquals(entity, childEntity);
    }
}
