package dev.morphia.test.mapping.codec.pojo;

import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.test.TestBase;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class TypeDataTest extends TestBase {
    @Test
    public void testWildcards() throws NoSuchFieldException {
        TypeData<?> typeData = TypeData.newInstance(WildCard.class.getDeclaredField("listOfLists"));

        Assert.assertEquals(typeData.getType(), List.class);
        List<TypeData<?>> typeParameters = typeData.getTypeParameters();

        typeData = typeParameters.get(0);
        Assert.assertEquals(typeData.getType(), List.class);
        typeParameters = typeData.getTypeParameters();

        typeData = typeParameters.get(0);
        Assert.assertEquals(typeData.getType(), String.class);

    }

    private static class WildCard {
        private List<? extends List<String>> listOfLists;
    }
}