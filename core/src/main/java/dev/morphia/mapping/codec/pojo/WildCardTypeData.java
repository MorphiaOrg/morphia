package dev.morphia.mapping.codec.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public static Builder builder(boolean upperBound) {
        return new Builder(upperBound);
    }

    @Override
    public Class getType() {
        TypeData<?> typeData = getTypeParameters().get(0);
        return typeData.getType();
    }

    public boolean isUpperBound() {
        return upperBound;
    }

    public static class Builder {
        private final boolean upperBound;
        private final List<TypeData<?>> typeParameters = new ArrayList<>();

        public Builder(boolean upperBound) {
            this.upperBound = upperBound;
        }

        public <S> Builder addTypeParameter(TypeData<S> typeParameter) {
            typeParameters.add(notNull("typeParameter", typeParameter));
            return this;
        }

        public WildCardTypeData build() {
            return new WildCardTypeData(upperBound, Collections.unmodifiableList(typeParameters));
        }

    }
}
