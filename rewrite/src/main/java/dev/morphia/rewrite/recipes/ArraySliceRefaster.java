package dev.morphia.rewrite.recipes;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

import dev.morphia.query.ArraySlice;

import org.openrewrite.java.template.RecipeDescriptor;

@RecipeDescriptor(name = "ArraySliceRefaster", description = "Migrates usages of the ArraySlice constructor to the factory methods  .")
public class ArraySliceRefaster {
    @RecipeDescriptor(name = "ArraySliceLimitOnly", description = "Migrates usages of the ArraySlice constructor to the ArraySlice.limit().")
    public static class LimitOnly {

        @BeforeTemplate
        public ArraySlice before(int value) {
            return new ArraySlice(value);
        }

        @AfterTemplate
        public ArraySlice after(int value) {
            return ArraySlice.limit(value);
        }
    }

    @RecipeDescriptor(name = "ArraySliceLimitAndSkip", description = "Migrates usages of the ArraySlice constructor to the ArraySlice" +
            ".skip().limit().")
    public static class LimitAndSkip {
        @BeforeTemplate
        public ArraySlice before(int value, int skip) {
            return new ArraySlice(value, skip);
        }

        @AfterTemplate
        public ArraySlice after(int value, int skip) {
            return ArraySlice.limit(value)
                    .skip(skip);
        }
    }
}
