package xyz.morphia.generics;

import org.junit.Assert;
import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.generics.model.AnotherChildEmbedded;
import xyz.morphia.generics.model.ChildEmbedded;
import xyz.morphia.generics.model.ChildEntity;
import xyz.morphia.query.FindOptions;

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
