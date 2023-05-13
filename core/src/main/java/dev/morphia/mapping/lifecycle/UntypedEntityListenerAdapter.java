package dev.morphia.mapping.lifecycle;

import java.lang.reflect.Method;
import java.util.List;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.Document;

/**
 * This class wraps the legacy {@link EntityListeners#value()} values with no fixed API to the more formalized {@link EntityListener}
 * interface.
 *
 * @hidden
 * @since 2.4.0
 * @morphia.internal
 */
@MorphiaInternal
public class UntypedEntityListenerAdapter extends EntityListenerAdapter {
    public UntypedEntityListenerAdapter(Class<?> type) {
        super(type);
    }

    void invoke(Class<?> annotation, Object entity, Document document, Datastore datastore) {
        List<Method> list = getMethods().get(annotation);
        if (list != null) {
            list.forEach(method -> {
                try {
                    method.invoke(getListener(), collectArgs(method, entity, document, datastore));
                } catch (IllegalArgumentException | ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
