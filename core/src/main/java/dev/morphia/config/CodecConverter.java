package dev.morphia.config;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.codecs.Codec;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class CodecConverter extends ClassNameConverter<Codec<?>> {
    @Override
    public Codec<?> convert(String value) throws IllegalArgumentException, NullPointerException {
        return (Codec<?>) super.convert(value);
    }
}
