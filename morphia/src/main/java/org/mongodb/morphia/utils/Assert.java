package org.mongodb.morphia.utils;


public final class Assert {
  private Assert() {
  }

  public static void raiseError(final String error) {
    throw new AssertionFailedException(error);
  }

  public static class AssertionFailedException extends RuntimeException {

    private static final long serialVersionUID = 435272532743543854L;

    public AssertionFailedException(final String detail) {
      super(detail);
    }

    public AssertionFailedException(final String detail, final Throwable e) {
      super(detail, e);
    }
  }

  public static void parametersNotNull(final String nameOfParameters, final Object... objects) {
    String msgPrefix = "At least one of the parameters ";
    final String msgSuffix = " is null.";

    if (objects != null) {
      if (objects.length == 1) {
        msgPrefix = "Parameter ";
      }
  
      for (final Object object : objects) {
        if (object == null) {
          raiseError(msgPrefix + "'" + nameOfParameters + "' " + msgSuffix);
        }
      }
    }
  }

  public static void parameterNotNull(final Object reference, final String nameOfParameter) {
    if (reference == null) {
      raiseError("Parameter '" + nameOfParameter + "' is not expected to be null.");
    }
  }

  public static void parameterNotEmpty(final Iterable obj, final String paramName) {
    if (!obj.iterator().hasNext()) {
      raiseError("Parameter '" + paramName + "' from type '" + obj.getClass().getName() + "' is expected to NOT be empty");
    }
  }

  public static void parameterNotEmpty(final String reference, final String nameOfParameter) {
    if (reference != null && reference.length() == 0) {
      raiseError("Parameter '" + nameOfParameter + "' is expected to NOT be empty.");
    }
  }

  public static void isEmpty(final Object[] array) {
    if (array == null) {
      raiseError("'array' is null");
    } else if (array.length != 0) {
      raiseError("'array' is not empty");
    }
  }
}