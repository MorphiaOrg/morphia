package dev.morphia.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @morphia.internal
 * @since 2.2
 */
@MorphiaInternal
public final class MorphiaInternals {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaInternals.class);
    private static final Map<DriverVersion, Boolean> versions = new HashMap<>();
    private static Boolean proxyClassesPresent;

    private MorphiaInternals() {
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
     * @param version the required minimum version
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
        v4_6_0,
        v5_0_0,
        v5_2_0,
    }
}
