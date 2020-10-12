/*
 * Copyright 2008-present MongoDB, Inc.
 * Copyright 2018 Cezary Bartosiak
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

package dev.morphia.mapping.codec;

import dev.morphia.mapping.MapperOptions;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.LocalTime;

/**
 * Converts the {@code LocalTime} values to and from the zone defined in {@link MapperOptions#getDateStorage()}
 *
 * @since 2.0
 */
public class MorphiaLocalTimeCodec implements Codec<LocalTime> {

    private static final int MILLI_MODULO = 1000000;

    @Override
    public LocalTime decode(BsonReader reader, DecoderContext decoderContext) {
        return LocalTime.ofNanoOfDay(reader.readInt64() * MILLI_MODULO);
    }

    @Override
    public void encode(BsonWriter writer, LocalTime value, EncoderContext encoderContext) {
        writer.writeInt64(value.toNanoOfDay() / MILLI_MODULO);
    }

    @Override
    public Class<LocalTime> getEncoderClass() {
        return LocalTime.class;
    }
}
