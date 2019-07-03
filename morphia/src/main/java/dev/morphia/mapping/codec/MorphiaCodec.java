package dev.morphia.mapping.codec;

import dev.morphia.mapping.codec.pojo.ClassModel;
import org.bson.codecs.Codec;

public interface MorphiaCodec<T> extends Codec<T> {
    ClassModel<T> getClassModel();
}
