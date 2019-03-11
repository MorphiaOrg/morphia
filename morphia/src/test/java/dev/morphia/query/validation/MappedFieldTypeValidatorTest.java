package dev.morphia.query.validation;

import org.junit.Test;
import dev.morphia.entities.EntityWithListsAndArrays;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MappedFieldTypeValidatorTest {
    @Test
    public void shouldAllowAListThatDoesNotContainNumbers() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("listOfIntegers");

        // expect
        assertThat(MappedFieldTypeValidator.isIterableOfNumbers(mappedField), is(true));
    }

    @Test
    public void shouldAllowArraysOfNumbers() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("arrayOfInts");

        // expect
        assertThat(MappedFieldTypeValidator.isArrayOfNumbers(mappedField), is(true));
    }

    @Test
    public void shouldRejectAListThatDoesNotContainNumbers() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("listOfStrings");

        // expect
        assertThat(MappedFieldTypeValidator.isIterableOfNumbers(mappedField), is(false));
    }

    @Test
    public void shouldRejectArraysOfStrings() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("arrayOfStrings");

        // expect
        assertThat(MappedFieldTypeValidator.isArrayOfNumbers(mappedField), is(false));
    }

}
