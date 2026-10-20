package dev.morphia.mapping.codec;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Wraps a codec registry so that looking up a codec by class is a single map lookup once the class has been seen. The
 * driver's registry caches codecs too, but builds several objects on every lookup to find the cached one, and Morphia
 * looks codecs up per value (array elements, map values, references, untyped properties). Results are the same: the
 * driver keeps one codec per class, and that's the codec cached here. Lookups that take a registry or type arguments
 * depend on their context and go straight to the wrapped registry.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public final class MemoizingCodecRegistry implements CodecRegistry {
    private final CodecRegistry delegate;
    private final Map<Class<?>, Codec<?>> codecs = new ConcurrentHashMap<>();

    /**
     * @param delegate the registry to look codecs up in
     */
    public MemoizingCodecRegistry(CodecRegistry delegate) {
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz) {
        Codec<?> codec = codecs.get(clazz);
        if (codec == null) {
            // not computeIfAbsent: building a codec looks up other codecs, which may land back here
            codec = delegate.get(clazz);
            Codec<?> existing = codecs.putIfAbsent(clazz, codec);
            if (existing != null) {
                codec = existing;
            }
        }
        return (Codec<T>) codec;
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, List<Type> typeArguments) {
        return delegate.get(clazz, typeArguments);
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        return delegate.get(clazz, registry);
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, List<Type> typeArguments, CodecRegistry registry) {
        return delegate.get(clazz, typeArguments, registry);
    }
}
