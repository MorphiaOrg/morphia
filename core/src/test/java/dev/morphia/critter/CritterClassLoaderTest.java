package dev.morphia.critter;

import java.io.InputStream;
import java.util.Collections;

import dev.morphia.mapping.Mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Pins the loading behavior generated critter code relies on.
 */
public class CritterClassLoaderTest {
    private static final ClassLoader PARENT = CritterClassLoaderTest.class.getClassLoader();

    @Test
    public void registeredClassesLoadChildFirst() throws Exception {
        CritterClassLoader loader = new CritterClassLoader(PARENT);
        loader.register(Mapper.class.getName(), classBytes(Mapper.class));

        Class<?> loaded = loader.loadClass(Mapper.class.getName());
        Assertions.assertNotSame(Mapper.class, loaded);
        Assertions.assertSame(loader, loaded.getClassLoader());
        Assertions.assertSame(loaded, loader.loadClass(Mapper.class.getName()));
    }

    @Test
    public void critterClassesLoadChildFirst() throws Exception {
        CritterClassLoader loader = new CritterClassLoader(PARENT);

        Assertions.assertSame(loader, loader.loadClass(Critter.class.getName()).getClassLoader());
    }

    @Test
    public void otherClassesDelegateToParent() throws Exception {
        CritterClassLoader loader = new CritterClassLoader(PARENT);

        Assertions.assertSame(Mapper.class, loader.loadClass(Mapper.class.getName()));
        Assertions.assertSame(String.class, loader.loadClass(String.class.getName()));
        Assertions.assertThrows(ClassNotFoundException.class, () -> loader.loadClass("dev.morphia.critter.DoesNotExist"));
        Assertions.assertThrows(ClassNotFoundException.class, () -> loader.loadClass("com.example.DoesNotExist"));
    }

    @Test
    public void typeDefinitionsAreReleasedOnceLoaded() throws Exception {
        CritterClassLoader loader = new CritterClassLoader(PARENT);
        loader.register(Mapper.class.getName(), classBytes(Mapper.class));
        Assertions.assertTrue(loader.getTypeDefinitions().containsKey(Mapper.class.getName()));

        loader.loadClass(Mapper.class.getName());
        Assertions.assertFalse(loader.getTypeDefinitions().containsKey(Mapper.class.getName()));
    }

    @Test
    public void childClassResourcesAreHidden() throws Exception {
        CritterClassLoader loader = new CritterClassLoader(PARENT);
        String resource = resourceName(Mapper.class);
        Assertions.assertNotNull(loader.getResource(resource));

        loader.register(Mapper.class.getName(), classBytes(Mapper.class));
        Assertions.assertNull(loader.getResourceAsStream(resource));
        loader.loadClass(Mapper.class.getName());
        Assertions.assertNull(loader.getResourceAsStream(resource));

        loader.loadClass(Critter.class.getName());
        Assertions.assertNull(loader.getResource(resourceName(Critter.class)));
    }

    @Test
    public void childClassResourcesAreStillEnumerated() throws Exception {
        CritterClassLoader loader = new CritterClassLoader(PARENT);
        String resource = resourceName(Mapper.class);
        loader.register(Mapper.class.getName(), classBytes(Mapper.class));
        loader.loadClass(Mapper.class.getName());

        Assertions.assertEquals(Collections.list(PARENT.getResources(resource)), Collections.list(loader.getResources(resource)));
    }

    @Test
    public void failedDefinitionsDoNotHideResources() throws Exception {
        CritterClassLoader loader = new CritterClassLoader(PARENT);
        String resource = resourceName(Critter.class);
        loader.register(Critter.class.getName(), new byte[] { 0, 1, 2, 3 });
        Assertions.assertNull(loader.getResource(resource));

        Assertions.assertThrows(ClassFormatError.class, () -> loader.loadClass(Critter.class.getName()));
        Assertions.assertNotNull(loader.getResource(resource));
    }

    private static String resourceName(Class<?> type) {
        return type.getName().replace('.', '/') + ".class";
    }

    private static byte[] classBytes(Class<?> type) throws Exception {
        try (InputStream stream = PARENT.getResourceAsStream(resourceName(type))) {
            return stream.readAllBytes();
        }
    }
}
