package dev.morphia.mapping.lazy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A utility class for detecting lazy loading support types
 */
public final class LazyFeatureDependencies {

    private static final Logger LOG = LoggerFactory.getLogger(LazyFeatureDependencies.class);
    private static Boolean proxyClassesPresent;

    private LazyFeatureDependencies() {
    }

    /**
     * Checks that the dependencies to support lazy proxies are present
     *
     * @return true the dependencies are found
     */
    public static boolean assertProxyClassesPresent() {
        if (proxyClassesPresent == null) {
            try {
                proxyClassesPresent = Class.forName("net.bytebuddy.implementation.InvocationHandlerAdapter") != null;
            } catch (ClassNotFoundException e) {
                proxyClassesPresent = false;
            }
            if (!proxyClassesPresent) {
                LOG.warn("Lazy loading impossible due to missing dependencies.");
            }
        }
        return proxyClassesPresent;
    }
}
