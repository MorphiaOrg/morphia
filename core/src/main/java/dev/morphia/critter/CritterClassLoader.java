package dev.morphia.critter;

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
 * Registered classes are loaded child-first so generated code links against them rather than the parent's copies;
 * everything else, Morphia's own classes included, is delegated to the parent, so there is one copy of each. A class's
 * bytes are released once it is defined. {@link #getResource(String)}, and so {@code getResourceAsStream}, returns
 * {@code null} for the {@code .class} file of a class this loader registered or defined; {@code getResources} still
 * lists the parent's copy, as ByteBuddy's {@code ChildFirst} loader did.
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

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> type = findLoadedClass(name);
            if (type == null && typeDefinitions.containsKey(name)) {
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
        byte[] bytes = typeDefinitions.remove(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        // The bytes are not restored if defineClass fails: the same bytes would fail the same way on a retry.
        // Only mark the name as defined (hiding the parent's .class resource) once the define succeeds.
        Class<?> type = defineClass(name, bytes, 0, bytes.length);
        definedTypes.add(name);
        return type;
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

    /**
     * Returns a copy of all registered type definitions keyed by binary class name.
     *
     * @return a map of class name to bytecode
     */
    public Map<String, byte[]> getTypeDefinitions() {
        return new HashMap<>(typeDefinitions);
    }
}
