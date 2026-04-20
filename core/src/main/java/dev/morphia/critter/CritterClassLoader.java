package dev.morphia.critter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.internal.MorphiaInternal;

import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;

/**
 * A class loader that supports registering and loading dynamically generated Critter classes from byte arrays.
 * 
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public class CritterClassLoader extends ByteArrayClassLoader.ChildFirst {

    /**
     * Creates a new CritterClassLoader with the given parent classloader.
     *
     * @param parent the parent classloader used for delegation
     */
    public CritterClassLoader(ClassLoader parent) {
        super(parent, Collections.emptyMap());
    }

    /**
     * Creates a new CritterClassLoader with the current thread's context classloader as parent.
     */
    public CritterClassLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Registers a class by its binary name and bytecode so it can be loaded by this class loader.
     *
     * @param name  the binary class name (e.g., {@code com.example.Foo})
     * @param bytes the class bytecode
     */
    public void register(String name, byte[] bytes) {
        typeDefinitions.put(name, bytes);
    }

    byte[] bytes(String name) throws ClassNotFoundException {
        // If already registered, return it
        byte[] existing = typeDefinitions.get(name);
        if (existing != null) {
            return existing;
        }

        // Try to load from resources if it's a project class
        if (shouldRegister(name)) {
            String resourceName = "%s.class".formatted(name.replace('.', '/'));
            // Try both this classloader and parent classloader
            java.io.InputStream stream = getResourceAsStream(resourceName);
            if (stream == null && getParent() != null) {
                stream = getParent().getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try (java.io.InputStream in = stream) {
                    byte[] data = in.readAllBytes();
                    register(name, data);
                    return data;
                } catch (java.io.IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Try to register from resources first if not already registered
        // Only register project classes to avoid LinkageError with third-party libraries
        if (!typeDefinitions.containsKey(name) && shouldRegister(name)) {
            java.net.URL resource = getResource("%s.class".formatted(name.replace('.', '/')));
            if (resource != null) {
                try {
                    register(name, resource.openStream().readAllBytes());
                } catch (java.io.IOException ignored) {
                }
            }
        }
        return super.findClass(name);
    }

    private boolean shouldRegister(String className) {
        // Only register classes from the dev.morphia.critter package
        // This avoids SecurityException (java.*, javax.*) and LinkageError (third-party libs)
        return className.startsWith("dev.morphia.critter.");
    }

    /**
     * Returns a copy of all registered type definitions keyed by binary class name.
     *
     * @return a map of class name to bytecode
     */
    public Map<String, byte[]> getTypeDefinitions() {
        return new HashMap<>(typeDefinitions);
    }
}
