package dev.morphia.internal;

import com.mongodb.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @morphia.internal
 * @since 2.2
 */
@SuppressWarnings("CheckStyle")
public final class Util {
    /**
     * @morphia.internal
     */
    private static final Map<DriverVersion, Boolean> versions = new HashMap<>();

    private Util() {
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
