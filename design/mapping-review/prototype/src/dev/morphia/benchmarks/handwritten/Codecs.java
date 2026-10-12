package dev.morphia.benchmarks.handwritten;

import java.lang.reflect.Field;
import java.util.List;

import dev.morphia.benchmarks.Models;
import dev.morphia.benchmarks.models.Address;
import dev.morphia.benchmarks.models.Profile;
import dev.morphia.mapping.Mapper;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Builds the prototype codecs for a benchmark model. {@code registry}/{@code mapper} are Morphia's, built exactly the way
 * {@code CodecBenchmark} builds them; the prototypes only use them for leaf codecs (e.g. LocalDateTime), cold fallbacks and
 * subtype hand-off.
 */
public final class Codecs {
    private Codecs() {
    }

    /** The upper bound: hand-written code of the shape a per-entity generator would emit. */
    @SuppressWarnings("unchecked")
    public static Codec<Object> handwritten(Models model, CodecRegistry registry, Mapper mapper) {
        CodecSupport.Context context = new CodecSupport.Context(registry, mapper);
        Codec<?> codec = switch (model) {
            case SIMPLE -> new SimpleEntityCodec(context);
            case NESTED -> {
                AddressCodec address = new AddressCodec(context);
                yield new NestedEntityCodec(context, address, new ProfileCodec(context, address));
            }
            default -> throw new UnsupportedOperationException("no hand-written codec for " + model);
        };
        return (Codec<Object>) codec;
    }

    /** The O2/O3 simulation: a flat, immutable runtime form interpreted by one shared loop. */
    @SuppressWarnings("unchecked")
    public static Codec<Object> flat(Models model, CodecRegistry registry, Mapper mapper) {
        CodecSupport.Context context = new CodecSupport.Context(registry, mapper);
        Codec<?>[] address = new Codec<?>[1];
        Codec<?>[] profile = new Codec<?>[1];
        StringListCodec stringList = new StringListCodec(context);
        java.util.function.Function<Field, Codec<?>> resolver = field -> {
            if (field.getType() == Address.class) {
                return address[0];
            } else if (field.getType() == Profile.class) {
                return profile[0];
            } else if (field.getType() == List.class) {
                return stringList;
            }
            return registry.get(field.getType());
        };
        address[0] = new FlatModelCodec<>(Address.class, context, resolver);
        profile[0] = new FlatModelCodec<>(Profile.class, context, resolver);
        return (Codec<Object>) new FlatModelCodec<>(model.type(), context, resolver);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static final class StringListCodec implements Codec<List> {
        private final CodecSupport.Context context;

        StringListCodec(CodecSupport.Context context) {
            this.context = context;
        }

        @Override
        public void encode(BsonWriter writer, List value, EncoderContext ctx) {
            CodecSupport.writeStringList(writer, value);
        }

        @Override
        public List decode(BsonReader reader, DecoderContext ctx) {
            return CodecSupport.readStringList(context, reader, ctx);
        }

        @Override
        public Class<List> getEncoderClass() {
            return List.class;
        }
    }
}
