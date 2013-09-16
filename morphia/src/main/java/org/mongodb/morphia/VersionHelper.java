package org.mongodb.morphia;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public final class VersionHelper {
  public static long nextValue(final Long oldVersion) {
    return oldVersion == null ? 1 : oldVersion + 1;
  }

}
