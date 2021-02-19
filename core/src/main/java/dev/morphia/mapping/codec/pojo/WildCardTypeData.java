package dev.morphia.mapping.codec.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.bson.assertions.Assertions.notNull;

/**
 * Represents a wild card type
 *
 * @morphia.internal
 * @since 2.1.5
 */
public class WildCardTypeData extends TypeData<Object> {
    private final boolean upperBound;

    WildCardTypeData(boolean upperBound, List<TypeData<?>> typeParameters) {
        super(Object.class, typeParameters);
        this.upperBound = upperBound;
    }

    /**
     * Creates a builder
     *
     * @param upperBound true if the type parameters represent an upper bound
     * @return the new builder
     */
    public static Builder builder(boolean upperBound) {
        return new Builder(upperBound);
    }

    @Override
    public Class getType() {
        TypeData<?> typeData = getTypeParameters().get(0);
        return typeData.getType();
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

    /**
     * A builder for WildCardTypeData
     */
    public static class Builder {
        private final boolean upperBound;
        private final List<TypeData<?>> typeParameters = new ArrayList<>();

        /**
         * Creates a builder
         *
         * @param upperBound true if the type parameters represent an upper bound
         */
        public Builder(boolean upperBound) {
            this.upperBound = upperBound;
        }

        /**
         * Adds a type parameter
         *
         * @param typeParameter the type parameter
         * @param <S>           the type of the type parameter
         * @return this
         */
        public <S> Builder addTypeParameter(TypeData<S> typeParameter) {
            typeParameters.add(notNull("typeParameter", typeParameter));
            return this;
        }

        /**
         * @return the new WildCardTypeData
         */
        public WildCardTypeData build() {
            return new WildCardTypeData(upperBound, Collections.unmodifiableList(typeParameters));
        }
    }
}
