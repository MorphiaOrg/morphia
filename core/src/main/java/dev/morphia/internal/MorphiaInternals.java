package dev.morphia.internal;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import com.mongodb.client.MongoClient;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.semver4j.Semver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @hidden
 * @morphia.internal
 * @since 2.2
 */
@MorphiaInternal
public final class MorphiaInternals {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaInternals.class);
    private static final Map<Semver, Boolean> versions = new HashMap<>();
    private static Boolean proxyClassesPresent;
    private static Semver driverVersion;

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
    public static <V> V tryInvoke(String version, Supplier<V> block) {
        if (getDriverVersion().isGreaterThanOrEqualTo(version)) {
            return block.get();
        }
        return null;
    }

    /**
     * @param version the required minimum version
     * @param block   the block to invoke
     * @return the block's result
     */
    public static <V> V tryInvoke(String version, Supplier<V> block, Supplier<V> fallback) {
        if (getDriverVersion().isGreaterThanOrEqualTo(version)) {
            return block.get();
        }
        return fallback.get();
    }

    public static Semver getDriverVersion() {
        if (driverVersion == null) {
            try {
                URL location = MongoClient.class.getProtectionDomain().getCodeSource().getLocation();
                URLConnection connection = location.openConnection();
                try (JarInputStream stream = new JarInputStream(connection.getInputStream())) {
                    Manifest manifest = stream.getManifest();
                    System.out.println("manifest.getMainAttributes() = " + manifest.getMainAttributes());
                    driverVersion = Semver.parse(manifest.getMainAttributes().getValue("Build-Version"));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return driverVersion;
    }
}
