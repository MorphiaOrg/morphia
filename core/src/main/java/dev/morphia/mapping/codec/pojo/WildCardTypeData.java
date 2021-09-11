package dev.morphia.mapping.codec.pojo;

import java.util.Objects;

/**
 * Represents a wild card type
 *
 * @param <T> the bounds type
 * @morphia.internal
 * @since 2.1.5
 */
@SuppressWarnings("unchecked")
public class WildCardTypeData<T> extends TypeData<T> {
    private final boolean upperBound;

    WildCardTypeData(TypeData<T> type, boolean upperBound) {
        super(type.getType(), type.getTypeParameters());
        this.upperBound = upperBound;
    }

    /**
     * Creates a builder
     *
     * @param bound
     * @param upperBound true if the type parameters represent an upper bound
     * @return the new builder
     */
    public static Builder builder(TypeData<?> bound, boolean upperBound) {
        return new Builder(bound, upperBound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), upperBound);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WildCardTypeData)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        WildCardTypeData that = (WildCardTypeData) o;
        return upperBound == that.upperBound;
    }

    /**
     * @return true if the type parameters represent an upper bound
     */
    public boolean isUpperBound() {
        return upperBound;
    }

    @Override
    public String toString() {
/*
        String value = type.getSimpleName();
        if (!typeParameters.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ", "<", ">");
            typeParameters.forEach(t -> {
                joiner.add(t.toString());
            });
            value += joiner;
        }
*/

        return (upperBound ? "? extends " : "? super ") + super.toString();
    }

    /**
     * A builder for WildCardTypeData
     */
    public static class Builder {
        private final boolean upperBound;
        private final TypeData typeData;

        /**
         * Creates a builder
         *
         * @param bound
         * @param upperBound true if the type parameters represent an upper bound
         */
        public Builder(TypeData bound, boolean upperBound) {
            this.typeData = bound;
            this.upperBound = upperBound;
        }

        /**
         * @return the new WildCardTypeData
         */
        public WildCardTypeData build() {
            return new WildCardTypeData(typeData, upperBound);
        }
    }
}
