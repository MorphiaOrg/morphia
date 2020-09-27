package dev.morphia.mapping;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.lang.annotation.Annotation;

class MorphiaShortCutProvider implements CodecProvider {
    private final Mapper mapper;
    private final MorphiaCodecProvider codecProvider;

    MorphiaShortCutProvider(Mapper mapper, MorphiaCodecProvider codecProvider) {
        this.mapper = mapper;
        this.codecProvider = codecProvider;
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        return hasAnnotation(clazz, Entity.class) || hasAnnotation(clazz, Embedded.class) || mapper.isMapped(clazz)
               ? codecProvider.get(clazz, registry)
               : null;
    }

    private <T> boolean hasAnnotation(Class<T> clazz, Class<? extends Annotation> ann) {
        return clazz.getAnnotation(ann) != null;
    }
}
