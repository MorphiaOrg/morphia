package dev.morphia.test.annotations;

import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Text;
import dev.morphia.annotations.Validation;
import dev.morphia.annotations.builders.AnnotationBuilder;
import dev.morphia.annotations.builders.CollationBuilder;
import dev.morphia.annotations.builders.FieldBuilder;
import dev.morphia.annotations.builders.IndexBuilder;
import dev.morphia.annotations.builders.IndexOptionsBuilder;
import dev.morphia.annotations.builders.IndexedBuilder;
import dev.morphia.annotations.builders.TextBuilder;
import dev.morphia.annotations.builders.ValidationBuilder;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static java.lang.String.format;
import static org.testng.Assert.assertNotNull;

public class AnnotationBuilderTest {
    @Test
    public void builders() throws NoSuchMethodException {
        compareFields(Index.class, IndexBuilder.class);
        compareFields(IndexOptions.class, IndexOptionsBuilder.class);
        compareFields(Indexed.class, IndexedBuilder.class);
        compareFields(Field.class, FieldBuilder.class);
        compareFields(Collation.class, CollationBuilder.class);
        compareFields(Text.class, TextBuilder.class);
        compareFields(Validation.class, ValidationBuilder.class);
    }

    private <T extends Annotation> void compareFields(Class<T> annotationType, Class<? extends AnnotationBuilder<T>> builder)
        throws NoSuchMethodException {

        for (Method method : annotationType.getDeclaredMethods()) {
            Method getter = builder.getDeclaredMethod(method.getName(), method.getReturnType());
            assertNotNull(getter, format("Looking for %s.%s(%s) on ", builder.getSimpleName(), method.getName(),
                method.getReturnType().getSimpleName()));
        }
    }

}
