package dev.morphia.query;

import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.sofia.Sofia;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public enum FilterOperator {

    WITHIN_CIRCLE("$center") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.center(prop, value);
        }
    },

    WITHIN_CIRCLE_SPHERE("$centerSphere") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.centerSphere(prop, value);
        }
    },

    WITHIN_BOX("$box") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.box(prop, value);
        }
    },

    EQUAL("$eq", "=", "==") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.eq(prop, value);
        }
    },

    NOT_EQUAL("$ne", "!=", "<>") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.ne(prop, value);
        }
    },

    GREATER_THAN("$gt", ">") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.gt(prop, value);
        }
    },

    GREATER_THAN_OR_EQUAL("$gte", ">=") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.gte(prop, value);
        }
    },

    LESS_THAN("$lt", "<") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.lt(prop, value);
        }
    },

    LESS_THAN_OR_EQUAL("$lte", "<=") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.lte(prop, value);
        }
    },

    EXISTS("$exists", "exists") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.exists(prop, value);
        }
    },

    TYPE("$type", "type") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.type(prop, value);
        }
    },

    NOT("$not") {
        @Override
        public Filter apply(final String prop, final Object value) {
            throw new UnsupportedOperationException(Sofia.translationNotCurrentlySupported());
        }
    },

    MOD("$mod", "mod") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.mod(prop, value);
        }
    },

    SIZE("$size", "size") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.size(prop, value);
        }
    },

    IN("$in", "in") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.in(prop, value);
        }
    },

    NOT_IN("$nin", "nin") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.nin(prop, value);
        }
    },

    ALL("$all", "all") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.all(prop, value);
        }
    },

    ELEMENT_MATCH("$elemMatch", "elem", "elemMatch") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.elemMatch(prop, value);
        }
    },

    WHERE("$where") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.where(prop, value);
        }
    },

    // GEO
    NEAR("$near", "near") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.near(prop, value);
        }
    },

    NEAR_SPHERE("$nearSphere") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.nearSphere(prop, value);
        }
    },

    GEO_NEAR("$geoNear", "geoNear") {
        @Override
        public Filter apply(final String prop, final Object value) {
            //TODO:  implement this
            throw new UnsupportedOperationException();

            //            return Filters.geoNear(prop, value);
        }
    },

    GEO_WITHIN("$geoWithin", "geoWithin") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.geoWithin(prop, value);
        }
    },

    INTERSECTS("$geoIntersects", "geoIntersects") {
        @Override
        public Filter apply(final String prop, final Object value) {
            return Filters.geoIntersects(prop, value);
        }
    };

    private final String value;
    private final List<String> filters;

    FilterOperator(final String val, final String... filterValues) {
        value = val;
        filters = Arrays.asList(filterValues);
    }

    /**
     * Creates a FilterOperator from a String
     *
     * @param operator the String to convert
     * @return the FilterOperator
     */
    public static FilterOperator fromString(final String operator) {
        final String filter = operator.trim().toLowerCase();
        for (FilterOperator filterOperator : FilterOperator.values()) {
            if (filterOperator.matches(filter)) {
                return filterOperator;
            }
        }
        throw new IllegalArgumentException(format("Unknown operator '%s'", operator));
    }

    public abstract Filter apply(final String prop, final Object value);

    /**
     * Returns true if the given filter matches the filters on this FilterOperator
     *
     * @param filter the filter to check
     * @return true if the given filter matches the filters on this FilterOperator
     */
    public boolean matches(final String filter) {
        return filter != null && filters.contains(filter.trim().toLowerCase());
    }

    /**
     * @return the value of this FilterOperator
     */
    public String val() {
        return value;
    }
}
