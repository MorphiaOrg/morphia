package org.mongodb.morphia.query;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.entities.EntityWithListsAndArrays;
import org.mongodb.morphia.entities.SimpleEntity;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.validation.ValidationFailure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mongodb.morphia.query.FilterOperator.ALL;
import static org.mongodb.morphia.query.FilterOperator.EQUAL;
import static org.mongodb.morphia.query.FilterOperator.EXISTS;
import static org.mongodb.morphia.query.FilterOperator.GEO_WITHIN;
import static org.mongodb.morphia.query.FilterOperator.IN;
import static org.mongodb.morphia.query.FilterOperator.MOD;
import static org.mongodb.morphia.query.FilterOperator.NOT_IN;
import static org.mongodb.morphia.query.FilterOperator.SIZE;
import static org.mongodb.morphia.query.QueryValidator.validateQuery;

public class QueryValidatorTest {
    @Test
    public void shouldNotErrorWhenValidateQueryCalledWithNullValue() {
        // this unit test is to drive fixing a null pointer in the logging code.  It's a bit stupid but it's an edge case that wasn't 
        // caught.
        // when this is called, don't error
        validateQuery(SimpleEntity.class, new Mapper(), new StringBuilder("name"), EQUAL, null, true, true);
    }

    // All of the following tests are whitebox, as I have retrofitted them afterwards.  I have no idea if this is the required 
    // functionality or not

    @Test
    public void shouldBeCompatibleIfValueIsNull() {
        // expect
        // frankly not sure we should just let nulls through
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, EQUAL, null, new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldBeCompatibleIfTypeIsNull() {
        // expect
        // frankly not sure we should just let nulls through
        assertThat(QueryValidator.isCompatibleForOperator(null, null, EQUAL, "value", new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldAllowBooleanValuesForExistsOperator() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, EXISTS, true, new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldNotAllowNonBooleanValuesForExistsOperator() {
        // given
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, SimpleEntity.class, EXISTS, "value",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldAllowSizeOperatorForListTypesAndIntegerValues() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("listOfIntegers");

        // expect
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, NullClass.class, SIZE, 3, new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldAllowSizeOperatorForArraysAndIntegerValues() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("arrayOfInts");

        // expect
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, NullClass.class, SIZE, 3, new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    //this used to fail
    public void shouldAllowSizeOperatorForArrayListTypesAndIntegerValues() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("arrayListOfIntegers");

        // expect
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, NullClass.class, SIZE, 3, new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldNotAllowSizeOperatorForNonListTypes() {
        // given
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("notAnArrayOrList");

        // expect
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, NullClass.class, SIZE, 3, new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldNotAllowSizeOperatorForNonIntegerValues() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, ArrayList.class, SIZE, "value", new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldAllowInOperatorForIterableMapAndArrayValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, IN, Arrays.asList(1, 2),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, IN, Collections.emptySet(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, IN, new HashMap<String, String>(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, IN, new int[0], new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldNotAllowOtherValuesForInOperator() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, String.class, IN, "value", new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldAllowNotInOperatorForIterableMapAndArrayValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, NOT_IN, Arrays.asList(1, 2),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, NOT_IN, Collections.emptySet(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, NOT_IN, new HashMap<String, String>(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, NOT_IN, new int[0], new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldNotAllowOtherValuesForNotInOperator() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, SimpleEntity.class, NOT_IN, "value",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldAllowModOperatorForArrayOfIntegerValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, MOD, new int[2], new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    //this used to fail
    public void shouldNotErrorIfModOperatorIsUsedWithZeroLengthArrayOfIntegerValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, MOD, new int[0], new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldNotAllowModOperatorWithNonIntegerArray() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, MOD, new String[]{"value"},
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    //this used to fail
    public void shouldNotErrorModOperatorWithArrayOfNullValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, MOD, new String[1], new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    //this used to fail
    public void shouldNotAllowModOperatorWithNonArrayValue() {
        assertThat(QueryValidator.isCompatibleForOperator(null, String.class, MOD, "value", new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldAllowGeoWithinOperatorWithAllAppropriateTrimmings() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, List.class, GEO_WITHIN, new BasicDBObject("$box", 1),
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldNotAllowGeoWithinWhenValueDoesNotContainKeyword() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, List.class, GEO_WITHIN, new BasicDBObject("notValidKey", 1),
                                                          new ArrayList<ValidationFailure>()),
                   is(false)
                  );
    }

    @Test
    public void shouldNotAllowGeoOperatorIfValueDoesNotContainCorrectField() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, List.class, GEO_WITHIN, new BasicDBObject("name", "value"),
                                                          new ArrayList<ValidationFailure>()),
                   is(false)
                  );
    }

    @Test
    public void shouldNotAllowGeoOperatorIfValueIsNotDBObject() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, List.class, GEO_WITHIN, "value", new ArrayList<ValidationFailure>()),
                   is(false));
    }

    private static class GeoEntity {
        private final int[] array = {1};
    }

    @Test
    public void shouldAllowAllOperatorForIterableMapAndArrayValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, ALL, Arrays.asList(1, 2),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, ALL, Collections.emptySet(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, ALL, new HashMap<String, String>(),
                                                          new ArrayList<ValidationFailure>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, ALL, new int[0], new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldNotAllowOtherValuesForAllOperator() {
        // given
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");

        // expect
        assertThat(QueryValidator.isCompatibleForOperator(mappedField,
                                                          SimpleEntity.class,
                                                          ALL,
                                                          "value",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldAllowValuesOfIntegerIfTypeIsInteger() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, int.class, EQUAL, new Integer(1), new ArrayList<ValidationFailure>()),
                   is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, Integer.class, EQUAL, new Integer(1), new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldAllowValuesOfIntegerOrLongIfTypeIsLong() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, Long.class, EQUAL, new Integer(1), new ArrayList<ValidationFailure>()),
                   is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, long.class, EQUAL, new Integer(1), new ArrayList<ValidationFailure>()),
                   is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, Long.class, EQUAL, new Long(1), new ArrayList<ValidationFailure>()),
                   is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, long.class, EQUAL, new Long(1), new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldNotAllowNonIntegerTypeIfValueIsInt() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, SimpleEntity.class, EQUAL, new Integer(1),
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowNonIntegerValueIfTypeIsInt() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, int.class, EQUAL, "some non int value",
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldAllowValuesOfIntegerIfTypeIsDouble() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, Double.class, EQUAL, new Integer(1), new ArrayList<ValidationFailure>()),
                   is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, double.class, EQUAL, new Integer(1), new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldAllowValuesOfLongIfTypeIsDouble() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, Double.class, EQUAL, new Long(1), new ArrayList<ValidationFailure>()),
                   is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, double.class, EQUAL, new Long(1), new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldRejectNonDoubleValuesIfTypeIsDouble() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, Double.class, EQUAL, "Not a double", new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldAllowValueOfPatternWithTypeOfString() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, String.class, EQUAL, Pattern.compile("."),
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    //this used to fail
    public void shouldNotAllowNonStringTypeWithValueOfPattern() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, Pattern.class, EQUAL, Pattern.compile("."),
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowStringValueWithTypeThatIsNotString() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, Integer.class, EQUAL, "value", new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldAllowValueWithEntityAnnotationAndTypeOfKey() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, Key.class, EQUAL, new SimpleEntity(), new ArrayList<ValidationFailure>()),
                   is(true));
    }

    @Test
    public void shouldNotAllowValueWithoutEntityAnnotationAndTypeOfKey() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, Key.class, EQUAL, "value", new ArrayList<ValidationFailure>()),
                   is(false));
    }

    @Test
    public void shouldAllowTypeThatMatchesKeyTypeValue() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("integer");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, Integer.class, EQUAL,
                                                          new Key<Number>(Integer.class, new ObjectId()),
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldNotAllowTypeThatDoesNotMatchKeyTypeValue() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, String.class, EQUAL,
                                                          new Key<Number>(Integer.class, new ObjectId()),
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldNotAllowNonKeyTypeWithKeyValue() {
        // expect
        MappedClass mappedClass = new MappedClass(EntityWithListsAndArrays.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("listOfIntegers");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, SimpleEntity.class, EQUAL, new Key<String>("kind", new ObjectId()),
                                                          new ArrayList<ValidationFailure>()), is(false));
    }

    @Test
    public void shouldAllowValuesOfList() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, List.class, EQUAL, new ArrayList<String>(),
                                                          new ArrayList<ValidationFailure>()), is(true));
    }

    @Test
    public void shouldRejectTypesAndValuesThatDoNotMatch() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, String.class, EQUAL, 1, new ArrayList<ValidationFailure>()),
                   is(false));
    }

    private static class NullClass {
    }
}