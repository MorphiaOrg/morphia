package dev.morphia.mapping.codec;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.TypeData;

import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.TypeWithTypeParameters;

/**
 * Provides property specific codecs for customized handling generally related to generics but not necessarily.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public abstract class MorphiaPropertyCodecProvider implements PropertyCodecProvider {

    protected TypeWithTypeParameters<?> getType(List<? extends TypeWithTypeParameters<?>> typeParameters, int position) {
        return typeParameters.size() > position
                ? typeParameters.get(position)
                : TypeData.get(Object.class);
    }
}
