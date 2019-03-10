package dev.morphia.mapping.lazy.proxy;


/**
 * @author Uwe Schäfer, (schaefer@thomas-daily.de)
 */
//CHECKSTYLE:OFF
public interface ProxiedReference {
    Class __getReferenceObjClass();

    boolean __isFetched();

    Object __unwrap();
}
