package dev.morphia.rewrite.recipes;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

import dev.morphia.query.ArraySlice;

import org.openrewrite.java.template.RecipeDescriptor;

@RecipeDescriptor(name = "ArraySliceRefaster", description = "Migrates usages of the `ArraySlice` constructor to the `ArraySlice.limit()`.")
public class ArraySliceRefaster {
    @BeforeTemplate
    public ArraySlice before(int value) {
        return new ArraySlice(value);
    }

    @AfterTemplate
    public ArraySlice after(int value) {
        return ArraySlice.limit(value);
    }
}
