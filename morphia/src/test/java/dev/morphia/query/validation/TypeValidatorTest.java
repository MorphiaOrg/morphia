package dev.morphia.query.validation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TypeValidatorTest {
    @Test
    public void shouldAcceptArrayListTypes() {
        // expect
        assertThat(CollectionTypeValidator.typeIsAListOrArray(ArrayList.class), is(true));
    }

    @Test
    public void shouldAcceptArrayTypes() {
        // expect
        assertThat(CollectionTypeValidator.typeIsAListOrArray(int[].class), is(true));
    }

    @Test
    public void shouldAcceptListTypes() {
        // expect
        assertThat(CollectionTypeValidator.typeIsAListOrArray(List.class), is(true));
    }

    @Test
    public void shouldAllowIterableTypesThatAreNotListsAndRejectOtherTypes() {
        // expect
        assertThat(CollectionTypeValidator.typeIsIterable(Set.class), is(true));
        assertThat(CollectionTypeValidator.typeIsIterable(Map.class), is(false));
        assertThat(CollectionTypeValidator.typeIsIterable(int[].class), is(false));
    }

    @Test
    public void shouldAllowMapTypesAndRejectOtherTypes() {
        // given
        assertThat(CollectionTypeValidator.typeIsMap(HashMap.class), is(true));
        assertThat(CollectionTypeValidator.typeIsMap(Map.class), is(true));

        assertThat(CollectionTypeValidator.typeIsMap(Set.class), is(false));
        assertThat(CollectionTypeValidator.typeIsMap(List.class), is(false));
        assertThat(CollectionTypeValidator.typeIsMap(int[].class), is(false));
    }

    @Test
    public void shouldRejectIterablesThatAreNotListOrArray() {
        // expect
        assertThat(CollectionTypeValidator.typeIsAListOrArray(Set.class), is(false));
    }

    @Test
    public void shouldRejectOtherTypes() {
        // expect
        assertThat(CollectionTypeValidator.typeIsAListOrArray(String.class), is(false));
    }

}
