package org.mongodb.morphia.query;


import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

/**
 * @author Scott Hernandez
 */
public enum FilterOperator {

    WITHIN_CIRCLE("$center"),

    WITHIN_CIRCLE_SPHERE("$centerSphere"),

    WITHIN_BOX("$box"),

    EQUAL("$eq", "=", "=="),

    NOT_EQUAL("$ne", "!=", "<>"),

    GREATER_THAN("$gt", ">"),

    GREATER_THAN_OR_EQUAL("$gte", ">="),

    LESS_THAN("$lt", "<"),

    LESS_THAN_OR_EQUAL("$lte", "<="),

    EXISTS("$exists", "exists"),

    TYPE("$type", "type"),

    NOT("$not"),

    MOD("$mod", "mod"),

    SIZE("$size", "size"),

    IN("$in", "in"),

    NOT_IN("$nin", "nin"),

    ALL("$all", "all"),

    ELEMENT_MATCH("$elemMatch", "elem", "elemMatch"),

    WHERE("$where"),

    // GEO
    NEAR("$near", "near"),

    NEAR_SPHERE("$nearSphere"),

    /**
     * @deprecated New in server version 2.4: $geoWithin replaces $within which is deprecated.
     */
    @Deprecated
    WITHIN("$within", "within"),
    
    GEO_NEAR("$geoNear", "geoNear"),
    
    GEO_WITHIN("$geoWithin", "geoWithin"),

    INTERSECTS("$geoIntersects", "geoIntersects");

    private final String value;
    private final List<String> filters;

    FilterOperator(final String val, final String... filterValues) {
        value = val;
        filters = Arrays.asList(filterValues);
    }

    public String val() {
        return value;
    }

    public boolean matches(final String filter) {
        return filter != null && filters.contains(filter.trim().toLowerCase());
    }

    public static FilterOperator fromString(final String operator) {
        final String filter = operator.trim().toLowerCase();
        for (FilterOperator filterOperator : FilterOperator.values()) {
            if (filterOperator.matches(filter)) {
                return filterOperator;
            }
        }
        throw new IllegalArgumentException(format("Unknown operator '%s'", operator));
    }
}