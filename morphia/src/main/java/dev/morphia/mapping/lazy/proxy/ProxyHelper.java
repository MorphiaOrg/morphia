package dev.morphia.mapping.lazy.proxy;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public final class ProxyHelper {

    private ProxyHelper() {
    }

    /**
     * If proxied, returns the unwrapped entity.
     *
     * @param entity the entity to check
     * @param <T>    the type of the entity
     * @return the bare entity
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final T entity) {
        if (isProxy(entity)) {
            return (T) asProxy(entity).__unwrap();
        }
        return entity;
    }

    /**
     * Checks if the Java reference passed is a proxy
     *
     * @param entity the entity to check
     * @return true if the reference is a proxied reference
     */
    public static boolean isProxy(final Object entity) {
        return (entity != null && isProxied(entity.getClass()));
    }

    private static <T> ProxiedReference asProxy(final T entity) {
        return ((ProxiedReference) entity);
    }

    /**
     * Checks if the Class passed is a ProxiedReference
     *
     * @param clazz the class to check
     * @return true if the class is a ProxiedReference
     */
    public static boolean isProxied(final Class<?> clazz) {
        return ProxiedReference.class.isAssignableFrom(clazz);
    }

    /**
     * Returns the class backing this entity
     *
     * @param entity the entity to check
     * @return if proxied, the Class of the proxied type.  the entity's Class otherwise
     */
    public static Class getReferentClass(final Object entity) {
        if (isProxy(entity)) {
            return asProxy(entity).__getReferenceObjClass();
        } else {
            return entity != null ? entity.getClass() : null;
        }
    }

    /**
     * Checks if the proxied entity has been fetched.
     *
     * @param entity the entity to check
     * @return true if the proxy hasn't been fetched.
     */
    public static boolean isUnFetched(final Object entity) {
        return !isFetched(entity);
    }

    /**
     * Checks if the proxied entity has been fetched.
     *
     * @param entity the entity to check
     * @return true if the proxy has been fetched.
     */
    public static boolean isFetched(final Object entity) {
        return entity == null || !isProxy(entity) || asProxy(entity).__isFetched();
    }
}
