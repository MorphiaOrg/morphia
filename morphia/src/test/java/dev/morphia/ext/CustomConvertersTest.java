/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */


package dev.morphia.ext;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DefaultDBDecoder;
import com.mongodb.DefaultDBEncoder;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.converters.IntegerConverter;
import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.entities.EntityWithListsAndArrays;
import dev.morphia.mapping.MappedField;
import dev.morphia.query.FindOptions;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


/**
 * @author Scott Hernandez
 */
public class CustomConvertersTest extends TestBase {

    @Test
    public void customerIteratorConverter() {
        getMorphia().getMapper().getConverters().addConverter(ListToMapConvert.class);
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        getMorphia().getMapper().getOptions().setStoreNulls(false);
        getMorphia().map(EntityWithListsAndArrays.class);
        final EntityWithListsAndArrays entity = new EntityWithListsAndArrays();
        entity.setListOfStrings(Arrays.asList("string 1", "string 2", "string 3"));
        getDs().save(entity);

        final DBObject dbObject = getDs().getCollection(EntityWithListsAndArrays.class).findOne();
        Assert.assertFalse(dbObject.get("listOfStrings") instanceof BasicDBList);

        final EntityWithListsAndArrays loaded = getDs().find(EntityWithListsAndArrays.class).find(new FindOptions().limit(1)).tryNext();
        Assert.assertEquals(entity.getListOfStrings(), loaded.getListOfStrings());
    }

    @Test
    public void mimeType() throws MimeTypeParseException {
        getMorphia().map(MimeTyped.class);
        getDs().ensureIndexes();
        MimeTyped entity = new MimeTyped();
        entity.name = "test name";
        entity.mimeType = new MimeType("text/plain"); //MimeTypeParseException
        final DBObject dbObject = getMorphia().toDBObject(entity);
        assertEquals("text/plain", dbObject.get("mimeType"));

        getDs().save(entity); // FAILS WITH ERROR HERE
    }

    @Before
    public void setup() {
        getMorphia().map(MyEntity.class, ValueObject.class);
    }

    @Test
    public void shouldUseSuppliedConverterToEncodeAndDecodeObject() {
        // given
        getMorphia().map(CharEntity.class);

        // when
        getDs().save(new CharEntity());

        // then check the representation in the database is a number
        final BasicDBObject dbObj = (BasicDBObject) getDs().getCollection(CharEntity.class).findOne();
        assertThat(dbObj.get("c"), is(instanceOf(int.class)));
        assertThat(dbObj.getInt("c"), is((int) 'a'));

        // then check CharEntity can be decoded from the database
        final CharEntity ce = getDs().find(CharEntity.class).find(new FindOptions().limit(1)).tryNext();
        assertThat(ce.c, is(notNullValue()));
        assertThat(ce.c.charValue(), is('a'));
    }

    /**
     * This test is green when {@link MyEntity#valueObject} is annotated with {@code @Property}, as in this case the field is not
     * serialized
     * at all. However, the bson encoder would fail to encode the object of type ValueObject (as shown by {@link
     * #testFullBSONSerialization()}).
     */
    @Test
    public void testDBObjectSerialization() {
        final MyEntity entity = new MyEntity(1L, new ValueObject(2L));
        final DBObject dbObject = getMorphia().toDBObject(entity);

        assertEquals(new BasicDBObject("_id", 1L).append("valueObject", 2L), dbObject);
        assertEquals(entity, getMorphia().fromDBObject(getDs(), MyEntity.class, dbObject));
    }

    /**
     * This test shows the full serialization, including bson encoding/decoding.
     */
    @Test
    public void testFullBSONSerialization() {
        final MyEntity entity = new MyEntity(1L, new ValueObject(2L));
        final DBObject dbObject = getMorphia().toDBObject(entity);

        final byte[] data = new DefaultDBEncoder().encode(dbObject);

        final DBObject decoded = new DefaultDBDecoder().decode(data, (DBCollection) null);
        final MyEntity actual = getMorphia().fromDBObject(getDs(), MyEntity.class, decoded);
        assertEquals(entity, actual);
    }

    static class CharacterToByteConverter extends TypeConverter implements SimpleValueConverter {
        CharacterToByteConverter() {
            super(Character.class, char.class);
        }

        @Override
        public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
            if (fromDBObject == null) {
                return null;
            }
            final IntegerConverter intConverter = new IntegerConverter();
            final Integer i = (Integer) intConverter.decode(targetClass, fromDBObject, optionalExtraInfo);
            return (char) i.intValue();
        }

        @Override
        public Object encode(final Object value, final MappedField optionalExtraInfo) {
            final Character c = (Character) value;
            return (int) c.charValue();
        }
    }

    @Converters(CharacterToByteConverter.class)
    private static class CharEntity {
        private final Character c = 'a';
        @Id
        private ObjectId id = new ObjectId();
    }

    /**
     * This test shows an issue with an {@code @Embedded} class A inheriting from an {@code @Embedded} class B that both have a Converter
     * assigned (A has AConverter, B has BConverter). <p> When an object (here MyEntity) has a property/field of type A and is
     * deserialized,
     * the deserialization fails with a "dev.morphia.mapping.MappingException: No usable constructor for A" . </p>
     */
    @Entity(noClassnameStored = true)
    private static class MyEntity {

        @Id
        private Long id;
        @Embedded
        private ValueObject valueObject;

        MyEntity() {
        }

        MyEntity(final Long id, final ValueObject valueObject) {
            this.id = id;
            this.valueObject = valueObject;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((valueObject == null) ? 0 : valueObject.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MyEntity other = (MyEntity) obj;
            if (id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!id.equals(other.id)) {
                return false;
            }
            if (valueObject == null) {
                if (other.valueObject != null) {
                    return false;
                }
            } else if (!valueObject.equals(other.valueObject)) {
                return false;
            }
            return true;
        }

    }

    @Embedded
    @Converters(ValueObject.BConverter.class)
    private static class ValueObject {

        private long value;

        ValueObject() {
        }

        ValueObject(final long value) {
            this.value = value;
        }

        static class BConverter extends TypeConverter implements SimpleValueConverter {

            BConverter() {
                this(ValueObject.class);
            }

            BConverter(final Class<? extends ValueObject> clazz) {
                super(clazz);
            }

            ValueObject create(final Long source) {
                return new ValueObject(source);
            }            @Override
            protected boolean isSupported(final Class<?> c, final MappedField optionalExtraInfo) {
                return c.isAssignableFrom(ValueObject.class);
            }

            @Override
            public ValueObject decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
                if (fromDBObject == null) {
                    return null;
                }
                return create((Long) fromDBObject);
            }



            @Override
            public Long encode(final Object value, final MappedField optionalExtraInfo) {
                if (value == null) {
                    return null;
                }
                final ValueObject source = (ValueObject) value;
                return source.value;
            }

        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (value ^ (value >>> 32));
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ValueObject other = (ValueObject) obj;
            return value == other.value;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [value=" + value + "]";
        }

    }

    @Entity
    @Converters(MimeTypeConverter.class)
    private static class MimeTyped {
        @Id
        private ObjectId id;
        private String name;
        private javax.activation.MimeType mimeType;
    }

    public static class MimeTypeConverter extends TypeConverter {
        public MimeTypeConverter() {
            super(MimeType.class);
        }

        @Override
        public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
            try {
                return new MimeType(((BasicDBObject) fromDBObject).getString("mimeType"));
            } catch (MimeTypeParseException ex) {
                return new MimeType();
            }
        }

        @Override
        public Object encode(final Object value, final MappedField optionalExtraInfo) {
            return ((MimeType) value).getBaseType();
        }
    }

    @SuppressWarnings("unchecked")
    private static class ListToMapConvert extends TypeConverter {
        @Override
        protected boolean isSupported(final Class c, final MappedField mf) {
            return (mf != null) && mf.isMultipleValues() && !mf.isMap();
        }

        @Override
        public Object decode(final Class<?> targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
            if (fromDBObject != null) {
                Map<String, Object> map = (Map<String, Object>) fromDBObject;
                List<Object> list = new ArrayList<Object>(map.size());
                for (Entry<String, Object> entry : map.entrySet()) {
                    list.add(Integer.parseInt(entry.getKey()), entry.getValue());
                }

                return list;
            }
            return null;
        }

        @Override
        public Object encode(final Object value, final MappedField optionalExtraInfo) {
            if (value != null) {
                Map<String, Object> map = new LinkedHashMap<String, Object>();
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    map.put(i + "", list.get(i));
                }
                return map;
            }

            return null;
        }
    }
}

