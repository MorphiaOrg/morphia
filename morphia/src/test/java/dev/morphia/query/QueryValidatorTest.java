package dev.morphia.query;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Serialized;
import dev.morphia.entities.EntityWithListsAndArrays;
import dev.morphia.entities.SimpleEntity;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.validation.ValidationFailure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static dev.morphia.query.FilterOperator.ALL;
import static dev.morphia.query.FilterOperator.EQUAL;
import static dev.morphia.query.FilterOperator.EXISTS;
import static dev.morphia.query.FilterOperator.GEO_WITHIN;
import static dev.morphia.query.FilterOperator.IN;
import static dev.morphia.query.FilterOperator.MOD;
import static dev.morphia.query.FilterOperator.NOT_IN;
import static dev.morphia.query.FilterOperator.SIZE;

public class QueryValidatorTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldAllowAllOperatorForIterableMapAndArrayValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, ALL, Arrays.asList(1, 2),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, ALL, Collections.emptySet(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, ALL, new HashMap<String, String>(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, ALL, new int[0],
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    // All of the following tests are whitebox, as I have retrofitted them afterwards.  I have no idea if this is the required
    // functionality or not

    @Test
    public void shouldAllowBooleanValuesForExistsOperator() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, EXISTS, true, new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldAllowGeoWithinOperatorWithAllAppropriateTrimmings() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, List.class, GEO_WITHIN, new BasicDBObject("$box", 1),
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldAllowInOperatorForIterableMapAndArrayValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, IN, Arrays.asList(1, 2),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, IN, Collections.emptySet(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, IN, new HashMap<String, String>(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null,
                                                          null,
                                                          SimpleEntity.class,
                                                          IN,
                                                          new int[0],
                                                          new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldAllowModOperatorForArrayOfIntegerValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, MOD, new int[2],
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldAllowNotInOperatorForIterableMapAndArrayValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, NOT_IN, Arrays.asList(1, 2),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, NOT_IN, Collections.emptySet(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, NOT_IN, new HashMap<String, String>(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null,
                                                          null, SimpleEntity.class, NOT_IN, new int[0], new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    //this used to fail
    public void shouldAllowSizeOperatorForArrayListTypesAndIntegerValues() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("arrayListOfIntegers");

        // expect
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass,
                                                          mappedField,
                                                          NullClass.class,
                                                          SIZE,
                                                          3,
                                                          new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldAllowSizeOperatorForArraysAndIntegerValues() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("arrayOfInts");

        // expect
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass,
                                                          mappedField,
                                                          NullClass.class,
                                                          SIZE,
                                                          3,
                                                          new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldAllowSizeOperatorForListTypesAndIntegerValues() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("listOfIntegers");

        // expect
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass,
                                                          mappedField,
                                                          NullClass.class,
                                                          SIZE,
                                                          3,
                                                          new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldAllowTypeThatMatchesKeyTypeValue() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("integer");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, Integer.class, EQUAL,
                                                          new Key<Number>(Integer.class, "Integer", new ObjectId()),
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldAllowValueOfPatternWithTypeOfString() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, String.class, EQUAL, Pattern.compile("."),
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldAllowValueWithEntityAnnotationAndTypeOfKey() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, Key.class, EQUAL, new SimpleEntity(),
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldAllowValuesOfIntegerIfTypeIsDouble() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, Double.class, EQUAL, 1, new ArrayList<ValidationFailure>()),
                   is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, double.class, EQUAL, 1, new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldAllowValuesOfIntegerIfTypeIsInteger() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, int.class, EQUAL, 1, new ArrayList<ValidationFailure>()),
                   is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, Integer.class, EQUAL, 1,
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldAllowValuesOfIntegerOrLongIfTypeIsLong() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, Long.class, EQUAL, 1, new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, long.class, EQUAL, 1, new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, Long.class, EQUAL, 1L, new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, long.class, EQUAL, 1L, new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldAllowValuesOfList() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, List.class, EQUAL, new ArrayList<String>(),
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldAllowValuesOfLongIfTypeIsDouble() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, Double.class, EQUAL, 1L, new ArrayList<ValidationFailure>()),
                   is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, null, double.class, EQUAL, 1L, new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldBeCompatibleIfTypeIsNull() {
        // expect
        // frankly not sure we should just let nulls through
        assertThat(QueryValidator.isCompatibleForOperator(null, null, null, EQUAL, "value", new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldBeCompatibleIfValueIsNull() {
        // expect
        // frankly not sure we should just let nulls through
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, EQUAL, null, new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldNotAllowGeoOperatorIfValueDoesNotContainCorrectField() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, List.class, GEO_WITHIN,
                                                          new BasicDBObject("name", "value"), new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldNotAllowGeoOperatorIfValueIsNotDBObject() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, List.class, GEO_WITHIN, "value",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowGeoWithinWhenValueDoesNotContainKeyword() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, List.class, GEO_WITHIN,
                                                          new BasicDBObject("notValidKey", 1), new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    //this used to fail
    public void shouldNotAllowModOperatorWithNonArrayValue() {
        assertThat(QueryValidator.isCompatibleForOperator(null, null, String.class, MOD, "value", new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldNotAllowModOperatorWithNonIntegerArray() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, MOD, new String[]{"value"},
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowNonBooleanValuesForExistsOperator() {
        // given
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, SimpleEntity.class, EXISTS, "value",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowNonIntegerTypeIfValueIsInt() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, SimpleEntity.class, EQUAL, 1,
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowNonIntegerValueIfTypeIsInt() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, int.class, EQUAL, "some non int value",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowNonKeyTypeWithKeyValue() {
        // expect
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("listOfIntegers");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, SimpleEntity.class, EQUAL,
                                                          new Key<String>(String.class, "kind", new ObjectId()),
                                                          new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    //this used to fail
    public void shouldNotAllowNonStringTypeWithValueOfPattern() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, Pattern.class, EQUAL, Pattern.compile("."),
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowOtherValuesForAllOperator() {
        // given
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");

        // expect
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, SimpleEntity.class, ALL, "value",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowOtherValuesForInOperator() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass,
                                                          mappedField,
                                                          String.class,
                                                          IN,
                                                          "value",
                                                          new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldNotAllowOtherValuesForNotInOperator() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, SimpleEntity.class, NOT_IN, "value",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowSizeOperatorForNonIntegerValues() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass,
                                                          mappedField,
                                                          ArrayList.class,
                                                          SIZE,
                                                          "value",
                                                          new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldNotAllowSizeOperatorForNonListTypes() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("notAnArrayOrList");

        // expect
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass,
                                                          mappedField,
                                                          NullClass.class,
                                                          SIZE,
                                                          3,
                                                          new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldNotAllowStringValueWithTypeThatIsNotString() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, Integer.class, EQUAL, "value",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowTypeThatDoesNotMatchKeyTypeValue() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, String.class, EQUAL,
                                                          new Key<Number>(Integer.class, "Integer", new ObjectId()),
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowValueWithoutEntityAnnotationAndTypeOfKey() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass,
                                                          mappedField,
                                                          Key.class,
                                                          EQUAL,
                                                          "value",
                                                          new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    //this used to fail
    public void shouldNotErrorIfModOperatorIsUsedWithZeroLengthArrayOfIntegerValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, MOD, new int[0],
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    //this used to fail
    public void shouldNotErrorModOperatorWithArrayOfNullValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, SimpleEntity.class, MOD, new String[1],
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotErrorWhenValidateQueryCalledWithNullValue() {
        // this unit test is to drive fixing a null pointer in the logging code.  It's a bit stupid but it's an edge case that wasn't
        // caught.
        // when this is called, don't error
        new PathTarget(new Mapper(), SimpleEntity.class, "name");
    }

    @Test
    public void shouldRejectNonDoubleValuesIfTypeIsDouble() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, Double.class, EQUAL, "Not a double",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldRejectTypesAndValuesThatDoNotMatch() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedClass, mappedField, String.class, EQUAL, 1,
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldReferToMappedClassInExceptionWhenFieldNotFound() {
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Could not resolve path '_id.notAField' against 'dev.morphia.entities.SimpleEntity'");
        new PathTarget(new Mapper(), SimpleEntity.class, "id.notAField").getTarget();
    }

    @Test
    public void shouldReferToMappedClassInExceptionWhenQueryingPastReferenceField() {
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Could not resolve path 'reference.name' against 'dev.morphia.query.QueryValidatorTest$WithReference'");
        new PathTarget(new Mapper(), WithReference.class, "reference.name").getTarget();
    }

    @Test
    public void shouldReferToMappedClassInExceptionWhenQueryingPastSerializedField() {
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Could not resolve path 'serialized.name' against "
                             + "'dev.morphia.query.QueryValidatorTest$WithSerializedField'");
        new PathTarget(new Mapper(), WithSerializedField.class, "serialized.name").getTarget();
    }

    private static class GeoEntity {
        private final int[] array = {1};
    }

    private static class NullClass {
    }

    private static class WithReference {
        @Reference
        private SimpleEntity reference;
    }

    private static class SerializableClass implements Serializable {
        private String name;
    }

    private static class WithSerializedField {
        @Serialized
        private SerializableClass serialized;
    }
}
