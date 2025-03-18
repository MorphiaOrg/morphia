package dev.morphia.rewrite.recipes;

import java.util.List;

import dev.morphia.query.ArraySlice;
import dev.morphia.rewrite.refaster.AfterTemplate;
import dev.morphia.rewrite.refaster.BeforeTemplate;

import org.openrewrite.java.template.RecipeDescriptor;

@RecipeDescriptor(name = "ArraySliceRefaster", description = "Migrates usages of the ArraySlice constructor to the factory methods  .")
public class ArraySliceRefaster {
    //    @RecipeDescriptor(name = "ArraySliceLimitOnly", description = "Migrates usages of the ArraySlice constructor to the ArraySlice
    //    .limit().")
    //    public static class LimitOnly {

    @SuppressWarnings("unused")
    public List<?> imports = List.of(ArraySlice.class);

    @BeforeTemplate
    String before = """
            public ArraySlice before(int value) {
                return new ArraySlice(value);
            }
            """;

    @AfterTemplate
    String after = """
            public ArraySlice after(int value) {
                return ArraySlice.limit(value);
            }
            """;
}
/*
 * @RecipeDescriptor(name = "ArraySliceLimitAndSkip", description = "Migrates usages of the ArraySlice constructor to the ArraySlice" +
 * ".skip().limit().")
 * public static class LimitAndSkip {
 * 
 * @BeforeTemplate
 * public ArraySlice before(int value, int skip) {
 * return new ArraySlice(value, skip);
 * }
 * 
 * @AfterTemplate
 * public ArraySlice after(int value, int skip) {
 * return ArraySlice.limit(value)
 * .skip(skip);
 * }
 * }
 * }
 */
