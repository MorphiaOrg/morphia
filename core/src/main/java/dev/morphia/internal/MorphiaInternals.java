package dev.morphia.internal;

import com.mongodb.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @morphia.internal
 * @since 2.2
 */
@SuppressWarnings("CheckStyle")
public final class MorphiaInternals {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaInternals.class);
    private static final Map<DriverVersion, Boolean> versions = new HashMap<>();
    private static Boolean proxyClassesPresent;
    private static Boolean kotlinAvailable;

    private MorphiaInternals() {
    }

    public static boolean kotlinAvailable() {
        if (kotlinAvailable == null) {
            try {
                Class.forName("kotlin.properties.ReadWriteProperty");
                kotlinAvailable = true;
            } catch (ClassNotFoundException e) {
                kotlinAvailable = false;
            }
        }
        return kotlinAvailable;
    }

    /**
     * Checks that the dependencies to support lazy proxies are present
     *
     * @return true the dependencies are found
     */
    public static boolean proxyClassesPresent() {
        if (proxyClassesPresent == null) {
            try {
                Class.forName("net.bytebuddy.implementation.InvocationHandlerAdapter");
                proxyClassesPresent = true;
            } catch (ClassNotFoundException e) {
                LOG.warn("Lazy loading impossible due to missing dependencies.");
                proxyClassesPresent = false;
            }
        }
        return proxyClassesPresent;
    }

    /**
     * @param version the required mininum version
     * @param block
     * @return
     */
    @Nullable
    public static <V> V tryInvoke(DriverVersion version, Supplier<V> block) {
        if (versions.get(version) == null) {
            try {
                return block.get();
            } catch (NoSuchMethodError e) {
                versions.put(version, false);
            }
        }
        return null;
    }

    /**
     * @param version the required mininum version
     * @param block
     * @return
     */
    public static <V> V tryInvoke(DriverVersion version, Supplier<V> block, Supplier<V> fallback) {
        if (versions.get(version) == null) {
            try {
                return block.get();
            } catch (NoSuchMethodError e) {
                versions.put(version, false);
            }
        }
        return fallback.get();
    }

    public enum DriverVersion {
        v4_0_0,
        v4_1_0,
        v4_2_0,
    }
}
