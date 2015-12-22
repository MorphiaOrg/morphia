package org.mongodb.morphia.mapping.lazy;


import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public final class LazyFeatureDependencies {

    private static final Logger LOG = MorphiaLoggerFactory.get(LazyFeatureDependencies.class);
    private static Boolean fulFilled;

    private LazyFeatureDependencies() {
    }

    /**
     * Checks that the dependencies to support lazy proxies are present
     *
     * @return true the dependencies are found
     */
    public static boolean assertDependencyFullFilled() {
        final boolean fulfilled = testDependencyFullFilled();
        if (!fulfilled) {
            LOG.warning("Lazy loading impossible due to missing dependencies.");
        }
        return fulfilled;
    }

    /**
     * Checks that the dependencies to support lazy proxies are present
     *
     * @return true the dependencies are found
     */
    public static boolean testDependencyFullFilled() {
        if (fulFilled != null) {
            return fulFilled;
        }
        try {
            fulFilled = Class.forName("net.sf.cglib.proxy.Enhancer") != null
                        && Class.forName("com.thoughtworks.proxy.toys.hotswap.HotSwapping")
                           != null;
        } catch (ClassNotFoundException e) {
            fulFilled = false;
        }
        return fulFilled;
    }

    /**
     * Creates a LazyProxyFactory
     *
     * @return the LazyProxyFactory
     */
    public static LazyProxyFactory createDefaultProxyFactory() {
        if (testDependencyFullFilled()) {
            final String factoryClassName = "org.mongodb.morphia.mapping.lazy.CGLibLazyProxyFactory";
            try {
                return (LazyProxyFactory) Class.forName(factoryClassName).newInstance();
            } catch (Exception e) {
                LOG.error("While instantiating " + factoryClassName, e);
            }
        }
        return null;
    }
}
