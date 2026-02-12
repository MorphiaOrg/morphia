package dev.morphia.config;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.codecs.configuration.CodecProvider;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class CodecProviderConverter extends ClassNameConverter<CodecProvider> {
    public CodecProviderConverter(MorphiaConfig morphiaConfig) {
        super(morphiaConfig);
    }

    @Override
    public CodecProvider convert(String value) throws IllegalArgumentException, NullPointerException {
        return (CodecProvider) super.convert(value);
    }
}
