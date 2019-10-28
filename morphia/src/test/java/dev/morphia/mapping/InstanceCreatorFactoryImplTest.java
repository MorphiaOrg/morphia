package dev.morphia.mapping;

import dev.morphia.TestBase;
import dev.morphia.testmodel.Rectangle;
import org.bson.codecs.pojo.InstanceCreator;
import org.bson.codecs.pojo.InstanceCreatorFactory;
import org.junit.Assert;
import org.junit.Test;

public class InstanceCreatorFactoryImplTest extends TestBase {
    @Test
    public void noarg() {
        InstanceCreatorFactory factory = new InstanceCreatorFactoryImpl(Rectangle.class);

        InstanceCreator creator = factory.create();

        Object instance = creator.getInstance();
        Assert.assertTrue(instance instanceof Rectangle);
    }
}