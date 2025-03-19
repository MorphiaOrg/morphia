package dev.morphia.rewrite.recipes;

import java.util.List;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

import dev.morphia.query.ArraySlice;
import dev.morphia.rewrite.refaster.RefasterAfterTemplate;
import dev.morphia.rewrite.refaster.RefasterBeforeTemplate;
import dev.morphia.rewrite.refaster.TemplateDescriptor;

import org.openrewrite.java.template.RecipeDescriptor;

@TemplateDescriptor(name = "ArraySliceMigration", description = "Migrates usages of the ArraySlice constructor to the factory methods.")
public class ArraySliceRefasterTemplate {
    @SuppressWarnings("unused")
    public List<?> imports = List.of(ArraySlice.class,
            RecipeDescriptor.class,
            AfterTemplate.class,
            BeforeTemplate.class);

    @TemplateDescriptor(name = "ArraySliceLimitOnly", description = "Migrates usages of the ArraySlice constructor to the ArraySlice.limit().")
    public static class LimitOnly {
        @RefasterBeforeTemplate
        String before = """
                public ArraySlice before(int value) {
                    return new ArraySlice(value);
                }
                """;

        @RefasterAfterTemplate
        String after = """
                public ArraySlice after(int value) {
                    return ArraySlice.limit(value);
                }
                """;
    }

    @TemplateDescriptor(name = "ArraySliceLimitAndSkip", description = "Migrates usages of the ArraySlice constructor to the ArraySlice.skip().limit().")
    public static class LimitAndSkip {

        @RefasterBeforeTemplate
        String before = """
                public ArraySlice before(int value, int skip) {
                    return new ArraySlice(value, skip);
                }
                """;

        @RefasterAfterTemplate
        String after = """
                public ArraySlice after(int value, int skip) {
                    return ArraySlice.limit(value)
                                     .skip(skip);
                }
                """;
    }
}
