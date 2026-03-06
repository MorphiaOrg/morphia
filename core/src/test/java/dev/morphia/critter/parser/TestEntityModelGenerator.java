package dev.morphia.critter.parser;

import java.lang.reflect.Method;

import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.critter.ClassfileOutput;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.ReflectiveMapper;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.NoInjection;

public class TestEntityModelGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(TestEntityModelGenerator.class);

    private final CritterEntityModel control;
    private final Mapper mapper = new ReflectiveMapper(new ManualMorphiaConfig());
    private final CritterClassLoader critterClassLoader = new CritterClassLoader(Thread.currentThread().getContextClassLoader());

    public TestEntityModelGenerator() {
        CritterEntityModel tmp;
        try {
            tmp = (CritterEntityModel) critterClassLoader
                    .loadClass("dev.morphia.critter.sources.ExampleEntityModelTemplate")
                    .getConstructor(Mapper.class)
                    .newInstance(mapper);
            ClassfileOutput.dump(critterClassLoader, "dev.morphia.critter.sources.ExampleEntityModelTemplate");
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        control = tmp;
    }

    // @Test(dataProvider = "methods")
    public void testEntityModel(String name, @NoInjection Method method) throws Exception {
        Object expected = method.invoke(control);
        Object actual = method.invoke(GeneratorTest.entityModel);
        Assert.assertEquals(actual, expected, method.getName() + " should return the same value");
    }

    @DataProvider(name = "methods")
    public Object[][] methods() {
        return GeneratorTest.methodNames(CritterEntityModel.class);
    }
}
