package dev.morphia.mapping.lazy.proxy;


import dev.morphia.Key;


/**
 * @author Uwe Schaefer, (schaefer@thomas-daily.de)
 */
//CHECKSTYLE:OFF
public interface ProxiedEntityReference extends ProxiedReference {
    Key<?> __getKey();
}
