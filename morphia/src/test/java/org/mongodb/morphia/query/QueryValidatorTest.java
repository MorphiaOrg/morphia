package org.mongodb.morphia.query;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.entities.SimpleEntity;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;

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
    public void shouldNotErrorWhenValueIsNull() {
        // this unit test is to drive fixing a null pointer in the logging code.  It's a bit stupid but it's an edge case that wasn't 
        // caught.
        // when this is called, don't error
        validateQuery(SimpleEntity.class, new Mapper(), new StringBuilder("name"), EQUAL, null, true, true);
    }

    //TODO: worryingly, when isCompatibleForOperator is hardcoded to return false, no tests fail (currently)
    // Ditto for when it's hardcoded for true.  Also no failures.  Not tested...

    // All of the following tests are whitebox, as I have retrofitted them afterwards.  I have no idea if this is the required 
    // functionality or not
    
    @Test
    public void shouldBeCompatibleIfValueIsNull() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, EQUAL, null), is(true));
    }

    @Test
    public void shouldBeCompatibleIfTypeIsNull() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, null, EQUAL, "value"), is(true));
    }

    @Test
    public void shouldAllowBooleanValuesForExistsOperator() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, EXISTS, true), is(true));
    }

    @Test
    public void shouldNotAllowNonBooleanValuesForExistsOperator() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, SimpleEntity.class, EXISTS, "value"), is(false));
    }

    @Test
    public void shouldAllowSizeOperatorForListTypesAndIntegerValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, List.class, SIZE, 3), is(true));
    }

    @Test
    @Ignore("So we don't allow arrays?")
    public void shouldAllowSizeOperatorForArraysAndIntegerValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, new int[0].getClass(), SIZE, 3), is(true));
    }

    @Test
    @Ignore("OK this is weird, I'd expect ArrayList to be OK. I think the logic is backwards")
    public void shouldAllowSizeOperatorForAllListTypesAndIntegerValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, ArrayList.class, SIZE, 3), is(true));
    }

    @Test
    public void shouldNotAllowSizeOperatorForNonListTypes() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, SimpleEntity.class, SIZE, 3), is(false));
    }

    @Test
    public void shouldNotAllowSizeOperatorForNonIntegerValues() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, ArrayList.class, SIZE, "value"), is(false));
    }

    @Test
    public void shouldAllowInOperatorForIterableMapAndArrayValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, IN, Arrays.asList(1,2)), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, IN, Collections.emptySet()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, IN, new HashMap<String, String>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, IN, new int[0]), is(true));
    }

    @Test
    @Ignore("the final 'false' fall-through does not work")
    public void shouldNotAllowOtherValuesForInOperator() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, String.class, IN, "value"), is(false));
    }

    @Test
    public void shouldAllowNotInOperatorForIterableMapAndArrayValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, NOT_IN, Arrays.asList(1, 2)), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, NOT_IN, Collections.emptySet()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, NOT_IN, new HashMap<String, String>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, NOT_IN, new int[0]), is(true));
    }

    @Test
    public void shouldNotAllowOtherValuesForNotInOperator() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, SimpleEntity.class, NOT_IN, "value"), is(false));
    }

    @Test
    public void shouldAllowModOperatorForArrayOfIntegerValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, MOD, new int[1]), is(true));
    }

    @Test
    @Ignore("bug waiting to happen")
    public void shouldNotErrorIfModOperatorIsUsedWithZeroLengthArrayOfIntegerValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, MOD, new int[0]), is(false));
    }

    @Test
    public void shouldNotAllowModOperatorWithNonIntegerArray() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, MOD, new String[]{"value"}), is(false));
    }

    @Test
    @Ignore("bug waiting to happen")
    public void shouldNotErrorModOperatorWithArrayOfNullValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, MOD, new String[1]), is(false));
    }

    @Test
    @Ignore("the final 'false' fall-through does not work")
    public void shouldNotAllowModOperatorWithNonArrayValue() {
        // expect
        // this is bad
//        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
//        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(null, String.class, MOD, "value"), is(false));
    }

    @Test
    public void shouldAllowGeoWithinOperatorWithAllAppropriateTrimmings() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, List.class, GEO_WITHIN, new BasicDBObject("$box", 1)), is(true));
    }

    @Test
    public void shouldNotAllowGeoOperatorIfValueDoesNotContainCorrectField() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, List.class, GEO_WITHIN, new BasicDBObject("name", "value")), 
                   is(false));
    }

    @Test
    public void shouldNotAllowGeoOperatorIfValueIsNotDBObject() {
        // expect
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, List.class, GEO_WITHIN, "value"), is(false));
    }

    private static class GeoEntity {
        private final int[] array = {1};
    }

    @Test
    public void shouldAllowAllOperatorForIterableMapAndArrayValues() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, ALL, Arrays.asList(1,2)), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, ALL, Collections.emptySet()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, ALL, new HashMap<String, String>()), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, SimpleEntity.class, ALL, new int[0]), is(true));
    }

    @Test
    public void shouldNotAllowOtherValuesForAllOperator() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, SimpleEntity.class, ALL, "value"), is(false));
    }

    @Test
    public void shouldOnlyAllowValuesOfIntegerIfTypeIsIntOrLong() {
        // expect
        // Op is only needed so it doesn't null pointer
        assertThat(QueryValidator.isCompatibleForOperator(null, Long.class, EQUAL, new Integer(1)), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, int.class, EQUAL, new Integer(1)), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, long.class, EQUAL, new Integer(1)), is(true));

        //for some insane reason, although the value needs to be an Integer, the type cannot be
//        assertThat(QueryValidator.isCompatibleForOperator(null, Integer.class, EQUAL, new Integer(1)), is(true));
    }

    @Test
    public void shouldNotAllowNonIntegerTypeIfValueIsInt() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, SimpleEntity.class, EQUAL, new Integer(1)), is(false));
    }

    @Test
    public void shouldOnlyAllowValuesOfIntegerIfTypeIsDouble() {
        // expect
        // Op is only needed so it doesn't null pointer
        assertThat(QueryValidator.isCompatibleForOperator(null, Double.class, EQUAL, new Integer(1)), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, double.class, EQUAL, new Integer(1)), is(true));
    }

    @Test
    public void shouldOnlyAllowValuesOfLongIfTypeIsDouble() {
        // expect
        // Op is only needed so it doesn't null pointer
        assertThat(QueryValidator.isCompatibleForOperator(null, Double.class, EQUAL, new Long(1)), is(true));
        assertThat(QueryValidator.isCompatibleForOperator(null, double.class, EQUAL, new Long(1)), is(true));
    }

    @Test
    @Ignore("the final 'false' fall-through does not work")
    public void shouldNotAllowNonDoubleTypeIfValueIsLong() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, long.class, EQUAL, new Long(1)), is(false));
    }

    @Test
    public void shouldAllowValueOfPatternWithTypeOfString() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, String.class, EQUAL, Pattern.compile(".")), is(true));
    }

    @Test
    @Ignore("the final 'false' fall-through does not work")
    public void shouldNotAllowNonStringTypeWithValueOfPattern() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, Pattern.class, EQUAL, Pattern.compile(".")), is(false));
    }

    @Test
    public void shouldNotAllowStringValueWithTypeOfString() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, Integer.class, EQUAL, "value"), is(false));
    }

    @Test
    public void shouldAllowValueWithEntityAnnotationAndTypeOfKey() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, Key.class, EQUAL, new SimpleEntity()), is(true));
    }

    @Test
    public void shouldNotAllowValueWithEntityAnnotationAndNonKeyType() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, String.class, EQUAL, new SimpleEntity()), is(false));
    }

    @Test
    public void shouldNotAllowValueWithoutEntityAnnotationAndTypeOfKey() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, Key.class, EQUAL, "value"), is(false));
    }

    @Test
    public void shouldAllowTypeThatMatchesKeyTypeValue() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, Integer.class, EQUAL, 
                                                          new Key<Number>(Integer.class, new ObjectId())), is(true));
    }

    @Test
    public void shouldNotAllowTypeThatDoesNotMatchKeyTypeValue() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, String.class, EQUAL, 
                                                          new Key<Number>(Integer.class, new ObjectId())), is(false));
    }

    @Test
    public void shouldNotAllowNonKeyTypeWithKeyValue() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, SimpleEntity.class, EQUAL, new Key<String>("kind", new ObjectId())), 
                   is(false));
    }

    @Test
    public void shouldAllowValuesOfList() {
        // expect
        assertThat(QueryValidator.isCompatibleForOperator(null, List.class, EQUAL, new ArrayList<String>()), is(true));
    }

    @Test
    public void shouldRejectTypesAndValuesThatDoNotMatch() {
        // expect
        MappedClass mappedClass = new MappedClass(SimpleEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("name");
        assertThat(QueryValidator.isCompatibleForOperator(mappedField, String.class, EQUAL, 1), is(false));
    }

    //the only branch I couldn't test was the bit using the mapper class
}