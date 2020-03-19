package dev.morphia.mapping.codec;


import morphia.org.bson.codecs.pojo.TypeData;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.util.List;

abstract class MorphiaPropertyCodecProvider implements PropertyCodecProvider {

    protected TypeWithTypeParameters<?> getType(final List<? extends TypeWithTypeParameters<?>> typeParameters, final int position) {
        return typeParameters.size() > position
               ? typeParameters.get(position)
               : TypeData.builder(Object.class).build();
    }
}
