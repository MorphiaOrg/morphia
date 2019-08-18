package dev.morphia.generics;

import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.generics.model.Another;
import dev.morphia.generics.model.Child;
import dev.morphia.generics.model.ChildEntity;
import dev.morphia.query.FindOptions;

import static java.util.Arrays.asList;

public class WildcardsTest extends TestBase {

    @Test
    public void example() {
        ChildEntity entity = new ChildEntity();
        entity.setEmbeddedList(asList(new Child("first"), new Child("second"), new Another("third")));
        getDs().save(entity);

        ChildEntity childEntity = getDs().find(ChildEntity.class)
                                         .execute(new FindOptions().limit(1))
                                         .next();

        Assert.assertEquals(entity, childEntity);
    }
}
