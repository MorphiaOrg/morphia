package org.mongodb.morphia.logging;


public interface LogrFactory {
  Logr get(Class<?> c);
}
