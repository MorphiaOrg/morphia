package dev.morphia.mapping;

import dev.morphia.TestBase;
import dev.morphia.mapping.codec.InstanceCreator;
import dev.morphia.testmodel.Rectangle;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class InstanceCreatorFactoryImplTest extends TestBase {
    @Test
    public void noarg() {
        List<MappedClass> list = getDs().getMapper().map(Rectangle.class);
        InstanceCreatorFactory factory = new InstanceCreatorFactoryImpl(list.get(0).getEntityModel());

        InstanceCreator creator = factory.create();

        Object instance = creator.getInstance();
        Assert.assertTrue(instance instanceof Rectangle);
    }
}