package dev.morphia.mapping.codec;

import org.bson.codecs.pojo.InstanceCreator;

/**
 * Marker interface for creators
 *
 * @param <T> the target type
 * @morphia.internal
 */
public interface MorphiaInstanceCreator<T> extends InstanceCreator<T> {
}
