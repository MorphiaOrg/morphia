package dev.morphia.mapping.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;

import org.bson.Document;

import static java.lang.String.format;

public class OnEntityListenerAdapter extends EntityListenerAdapter {
    private OnEntityListenerAdapter(Class<?> listenerType) {
        super(listenerType);
    }

    @Nullable
    public static OnEntityListenerAdapter listen(Class<?> type) {
        OnEntityListenerAdapter adapter = new OnEntityListenerAdapter(type);
        boolean annotatedMethods = adapter.getMethods().values().stream().anyMatch(methods -> !methods.isEmpty());
        return annotatedMethods ? adapter : null;
    }

    @Override
    void invoke(Class<? extends Annotation> annotation, Object entity, Document document, Datastore datastore) {
        List<Method> list = getMethods().get(annotation);
        if (list != null) {
            list
                    .forEach(method -> {
                        try {
                            method.invoke(entity, collectArgs(method, entity, document, datastore));
                        } catch (IllegalArgumentException e) {
                            if (e.getMessage().equals("object is not an instance of declaring class")) {
                                throw new RuntimeException(format("%s is not an instance of the method's type: %s",
                                        entity.getClass().getName(), method.getDeclaringClass().getName()), e);
                            }
                            throw new RuntimeException("Failed to invoke method: " + method, e);
                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException("Failed to invoke method: " + method, e);
                        }
                    });
        }
    }

}
