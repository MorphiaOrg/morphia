package org.mongodb.morphia.logging.jdk;


import org.mongodb.morphia.logging.Logr;
import org.mongodb.morphia.logging.LogrFactory;


public class JDKLoggerFactory implements LogrFactory {

  public Logr get(final Class<?> c) {
    return new JDKLogger(c);
  }

}
