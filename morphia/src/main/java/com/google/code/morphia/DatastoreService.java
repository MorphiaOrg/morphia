package com.google.code.morphia;


/**
 * @author Scott Hernnadez
 */
public class DatastoreService {
  private static final Morphia   mor;
  private static Datastore ds;

  static {
    mor = new Morphia();
    ds = mor.createDatastore("test");
  }

  private DatastoreService() {

  }

  /**
   * Connects to "test" database on localhost by default
   */
  public static Datastore getDatastore() {
    return ds;
  }

  public static void setDatabase(final String dbName) {
    if (!ds.getDB().getName().equals(dbName)) {
      ds = mor.createDatastore(dbName);
    }
  }

  public static void mapClass(final Class c) {
    mor.map(c);
  }

  public static void mapClasses(final Class[] classes) {
    for (final Class c : classes) {
      mapClass(c);
    }
  }

  public static void mapPackage(final String pkg) {
    mor.mapPackage(pkg, true);
  }
}