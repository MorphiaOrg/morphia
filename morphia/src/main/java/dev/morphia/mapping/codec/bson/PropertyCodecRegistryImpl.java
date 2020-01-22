/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia.mapping.codec.bson;

import dev.morphia.mapping.codec.MorphiaCollectionPropertyCodecProvider;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.util.ArrayList;
import java.util.List;

public class PropertyCodecRegistryImpl implements PropertyCodecRegistry {
    private final List<PropertyCodecProvider> propertyCodecProviders;

    public PropertyCodecRegistryImpl(final Codec<?> pojoCodec, final CodecRegistry codecRegistry,
                                     final List<PropertyCodecProvider> propertyCodecProviders) {
        List<PropertyCodecProvider> augmentedProviders = new ArrayList<PropertyCodecProvider>();
        if (propertyCodecProviders != null) {
            augmentedProviders.addAll(propertyCodecProviders);
        }
        augmentedProviders.add(new CollectionPropertyCodecProvider());
        augmentedProviders.add(new MorphiaCollectionPropertyCodecProvider());
        augmentedProviders.add(new EnumPropertyCodecProvider(codecRegistry));
        augmentedProviders.add(new FallbackPropertyCodecProvider(pojoCodec, codecRegistry));
        this.propertyCodecProviders = augmentedProviders;
    }

    @Override
    public <S> Codec<S> get(final TypeWithTypeParameters<S> type) {
        for (PropertyCodecProvider propertyCodecProvider : propertyCodecProviders) {
            Codec<S> codec = propertyCodecProvider.get(type, this);
            if (codec != null) {
                return codec;
            }
        }
        return null;
    }

    private static final class FallbackPropertyCodecProvider implements PropertyCodecProvider {
        private final CodecRegistry codecRegistry;
        private final Codec<?> codec;

        FallbackPropertyCodecProvider(final Codec<?> codec, final CodecRegistry codecRegistry) {
            this.codec = codec;
            this.codecRegistry = codecRegistry;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <S> Codec<S> get(final TypeWithTypeParameters<S> type, final PropertyCodecRegistry propertyCodecRegistry) {
            Class<S> clazz = type.getType();
            if (clazz == codec.getEncoderClass()) {
                return (Codec<S>) codec;
            }
            return codecRegistry.get(type.getType());
        }
    }
}
