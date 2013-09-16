package org.mongodb.morphia.mapping.lazy.proxy;


/**
 * @author Uwe Schäfer, (schaefer@thomas-daily.de)
 */
public interface ProxiedReference {
  boolean __isFetched();

  Class __getReferenceObjClass();

  Object __unwrap();
}
