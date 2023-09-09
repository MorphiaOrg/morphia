package dev.morphia.mapping.codec.pojo;

import java.util.Objects;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Represents a wild card type
 *
 * @param <T> the bounds type
 * @morphia.internal
 * @since 2.1.5
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class WildCardTypeData<T> extends TypeData<T> {
    private final boolean upperBound;

    WildCardTypeData(TypeData<T> type, boolean upperBound) {
        super(type.getType(), type.getTypeParameters());
        this.upperBound = upperBound;
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

    @Override
    public String toString() {
        return (upperBound ? "? extends " : "? super ") + super.toString();
    }
}
