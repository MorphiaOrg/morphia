package dev.morphia.mapping.codec.pojo;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodModel {
    private Map<Class<? extends Annotation>, List<Annotation>> annotations = new HashMap<>();

    public boolean hasAnnotation(final Class<? extends Annotation> annotationClass) {
        return annotations.get(annotationClass) != null;
    }
}
