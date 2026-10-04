package dev.morphia.critter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * A class loader that supports registering and loading dynamically generated Critter classes from byte arrays.
 * <p>
 * Registered classes, and classes from the {@code dev.morphia.critter} package, are loaded child-first so generated
 * code links against them rather than the parent's copies; everything else is delegated to the parent. A class's
 * bytes are released once it is defined, and the parent's {@code .class} resources for classes this loader defines
 * are hidden.
 *
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public class CritterClassLoader extends ClassLoader {
    static {
        registerAsParallelCapable();
    }

    private final Map<String, byte[]> typeDefinitions = new ConcurrentHashMap<>();
    private final Set<String> definedTypes = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new CritterClassLoader with the given parent classloader.
     *
     * @param parent the parent classloader used for delegation
     */
    public CritterClassLoader(ClassLoader parent) {
        super(parent);
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
            InputStream stream = getResourceAsStream(resourceName);
            if (stream == null && getParent() != null) {
                stream = getParent().getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try (InputStream in = stream) {
                    byte[] data = in.readAllBytes();
                    register(name, data);
                    return data;
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> type = findLoadedClass(name);
            if (type == null && (typeDefinitions.containsKey(name) || shouldRegister(name))) {
                try {
                    type = findClass(name);
                } catch (ClassNotFoundException e) {
                    // not available here; fall back to the parent
                }
            }
            if (type == null) {
                return super.loadClass(name, resolve);
            }
            if (resolve) {
                resolveClass(type);
            }
            return type;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Try to register from resources first if not already registered
        // Only register project classes to avoid LinkageError with third-party libraries
        if (!typeDefinitions.containsKey(name) && shouldRegister(name)) {
            URL resource = getParent() != null ? getParent().getResource("%s.class".formatted(name.replace('.', '/'))) : null;
            if (resource != null) {
                try (InputStream in = resource.openStream()) {
                    register(name, in.readAllBytes());
                } catch (IOException ignored) {
                }
            }
        }
        byte[] bytes = typeDefinitions.remove(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        definedTypes.add(name);
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    @Nullable
    public URL getResource(String name) {
        if (name.endsWith(".class")) {
            String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
            if (typeDefinitions.containsKey(className) || definedTypes.contains(className)) {
                return null;
            }
        }
        return super.getResource(name);
    }

    private boolean shouldRegister(String className) {
        // Only register classes from the dev.morphia.critter package
        // This avoids SecurityException (java.*, javax.*) and LinkageError (third-party libs).
        // NestmateAccessorRegistry must be excluded: it uses a static map that must be shared across
        // classloaders (the generator registers via the parent CL; generated models read via this CL).
        // Excluding it here lets child-first loading fall back to the parent for a single shared instance.
        return className.startsWith("dev.morphia.critter.")
                && !className.equals("dev.morphia.critter.parser.generator.NestmateAccessorRegistry");
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
