package dev.morphia;

import java.lang.annotation.Annotation;

/**
 * @deprecated the noop default methods have been moved to the interface. This class is now vestigial.
 */
@Deprecated(since = "2.0", forRemoval = true)
public class AbstractEntityInterceptor implements EntityInterceptor {
    @Override
    public boolean hasAnnotation(Class<? extends Annotation> type) {
        return false;
    }
}
