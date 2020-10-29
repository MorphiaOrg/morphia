package dev.morphia.mapping.codec;


import dev.morphia.mapping.codec.pojo.TypeData;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.util.List;

abstract class MorphiaPropertyCodecProvider implements PropertyCodecProvider {

    protected TypeWithTypeParameters<?> getType(List<? extends TypeWithTypeParameters<?>> typeParameters, int position) {
        return typeParameters.size() > position
               ? typeParameters.get(position)
               : TypeData.builder(Object.class).build();
    }
}
