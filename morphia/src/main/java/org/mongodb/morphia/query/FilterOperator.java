package org.mongodb.morphia.query;


/**
 * @author Scott Hernandez
 */
public enum FilterOperator {
  NEAR("$near"),
  NEAR_SPHERE("$nearSphere"),
  WITHIN("$within"),
  WITHIN_CIRCLE("$center"),
  WITHIN_CIRCLE_SPHERE("$centerSphere"),
  WITHIN_BOX("$box"),
  EQUAL("$eq"),
  GEO_WITHIN("$geoWithin"),
  GREATER_THAN("$gt"),
  GREATER_THAN_OR_EQUAL("$gte"),
  LESS_THAN("$lt"),
  LESS_THAN_OR_EQUAL("$lte"),
  EXISTS("$exists"),
  TYPE("$type"),
  NOT("$not"),
  MOD("$mod"),
  SIZE("$size"),
  IN("$in"),
  NOT_IN("$nin"),
  ALL("$all"),
  ELEMENT_MATCH("$elemMatch"),
  NOT_EQUAL("$ne"),
  WHERE("$where");

  private final String value;

  FilterOperator(final String val) {
    value = val;
  }

  private boolean equals(final String val) {
    return value.equals(val);
  }

  public String val() {
    return value;
  }

  public static FilterOperator fromString(final String val) {
    for (int i = 0; i < values().length; i++) {
      final FilterOperator fo = values()[i];
      if (fo.equals(val)) {
        return fo;
      }
    }
    return null;
  }
}