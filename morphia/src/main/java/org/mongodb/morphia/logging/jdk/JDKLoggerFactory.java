package org.mongodb.morphia.logging.jdk;


import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.LoggerFactory;


public class JDKLoggerFactory implements LoggerFactory {

  public Logger get(final Class<?> c) {
    return new JDKLogger(c);
  }

}
