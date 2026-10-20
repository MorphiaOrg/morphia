package dev.morphia.mapping.codec;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.codecs.Codec;
import org.bson.codecs.StringCodec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMemoizingCodecRegistry {
    @Test
    void looksEachClassUpOnce() {
        CountingRegistry delegate = new CountingRegistry(CodecRegistries.fromCodecs(new StringCodec()));
        MemoizingCodecRegistry registry = new MemoizingCodecRegistry(delegate);

        Codec<String> first = registry.get(String.class);
        Assertions.assertSame(first, registry.get(String.class));
        Assertions.assertSame(first, registry.get(String.class));
        Assertions.assertEquals(1, delegate.byClass.get(), "only the first lookup reaches the wrapped registry");
    }

    @Test
    void doesNotCacheFailures() {
        CountingRegistry delegate = new CountingRegistry(CodecRegistries.fromCodecs(new StringCodec()));
        MemoizingCodecRegistry registry = new MemoizingCodecRegistry(delegate);

        Assertions.assertThrows(CodecConfigurationException.class, () -> registry.get(Integer.class));
        Assertions.assertThrows(CodecConfigurationException.class, () -> registry.get(Integer.class));
        Assertions.assertEquals(2, delegate.byClass.get());
    }

    @Test
    void passesContextualLookupsThrough() {
        CountingRegistry delegate = new CountingRegistry(CodecRegistries.fromCodecs(new StringCodec()));
        MemoizingCodecRegistry registry = new MemoizingCodecRegistry(delegate);

        registry.get(String.class, registry);
        registry.get(String.class, registry);
        Assertions.assertEquals(2, delegate.contextual.get(), "lookups with a registry depend on it, so aren't cached");
    }

    private static final class CountingRegistry implements CodecRegistry {
        private final CodecRegistry delegate;
        private final AtomicInteger byClass = new AtomicInteger();
        private final AtomicInteger contextual = new AtomicInteger();

        private CountingRegistry(CodecRegistry delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> Codec<T> get(Class<T> clazz) {
            byClass.incrementAndGet();
            return delegate.get(clazz);
        }

        @Override
        public <T> Codec<T> get(Class<T> clazz, List<Type> typeArguments) {
            return delegate.get(clazz, typeArguments);
        }

        @Override
        public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
            contextual.incrementAndGet();
            return delegate.get(clazz, registry);
        }
    }
}
