package org.mongodb.morphia.mapping.lazy.proxy;


import org.mongodb.morphia.Key;


/**
 * @author Uwe Schaefer, (schaefer@thomas-daily.de)
 */
public interface ProxiedEntityReference extends ProxiedReference {
  Key<?> __getKey();
}
