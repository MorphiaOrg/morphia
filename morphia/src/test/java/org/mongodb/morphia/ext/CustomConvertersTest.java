/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.mongodb.morphia.ext;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DefaultDBDecoder;
import com.mongodb.DefaultDBEncoder;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.converters.IntegerConverter;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

import static org.junit.Assert.assertEquals;


/**
 * @author Scott Hernandez
 */
public class CustomConvertersTest extends TestBase {

    static class CharacterToByteConverter extends TypeConverter implements SimpleValueConverter {
        public CharacterToByteConverter() {
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
    static class CharEntity {
        @Id
        private ObjectId id = new ObjectId();
        private final Character c = 'a';
    }

    @Test
    public void testIt() {
        getMorphia().map(CharEntity.class);

        getDs().save(new CharEntity());
        final CharEntity ce = getDs().find(CharEntity.class).get();
        Assert.assertNotNull(ce.c);
        assertEquals('a', ce.c.charValue());

        final BasicDBObject dbObj = (BasicDBObject) getDs().getCollection(CharEntity.class).findOne();
        Assert.assertTrue(dbObj.getInt("c") == (int) 'a');
    }

    /**
     * This test shows an issue with an {@code @Embedded} class A inheriting from an {@code @Embedded} class B that both have a Converter
     * assigned (A has AConverter, B has BConverter). <p> When an object (here MyEntity) has a property/field of type A and is deserialized,
     * the deserialization fails with a "org.mongodb.morphia.mapping.MappingException: No usable constructor for A" . </p>
     */

    @Entity(noClassnameStored = true)
    private static class MyEntity {

        @Id
        private Long id;
        @Embedded
        private A a;

        public MyEntity() {
        }

        public MyEntity(final Long id, final A a) {
            this.id = id;
            this.a = a;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((a == null) ? 0 : a.hashCode());
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
            if (a == null) {
                if (other.a != null) {
                    return false;
                }
            } else if (!a.equals(other.a)) {
                return false;
            }
            return true;
        }

    }

    @Converters(B.BConverter.class)
    @Embedded
    private static class B {

        static class BConverter extends TypeConverter implements SimpleValueConverter {

            public BConverter() {
                this(B.class);
            }

            public BConverter(final Class<? extends B> clazz) {
                super(clazz);
            }

            @Override
            public B decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
                if (fromDBObject == null) {
                    return null;
                }
                return create((Long) fromDBObject);
            }

            protected B create(final Long source) {
                return new B(source);
            }

            @Override
            public Long encode(final Object value, final MappedField optionalExtraInfo) {
                if (value == null) {
                    return null;
                }
                final B source = (B) value;
                return source.value;
            }

        }

        private long value;

        public B() {
        }

        public B(final long value) {
            this.value = value;
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
            final B other = (B) obj;
            return value == other.value;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [value=" + value + "]";
        }

    }

    @Converters(A.AConverter.class)
    @Embedded
    private static class A extends B {

        static final class AConverter extends BConverter {

            public AConverter() {
                super(A.class);
            }

            @Override
            protected A create(final Long source) {
                return new A(source);
            }

        }

        public A() {
        }

        public A(final long value) {
            super(value);
        }
    }

    @Before
    public void setup() {
        getMorphia().map(MyEntity.class);
        getMorphia().map(B.class);
        getMorphia().map(A.class);
    }

    /**
     * This test is green when {@link MyEntity#a} is annotated with {@code @Property}, as in this case the field is not serialized at all.
     * However, the bson encoder would fail to encode the object of type A (as shown by {@link #testFullBSONSerialization()}).
     */
    @Test
    public void testDBObjectSerialization() {
        final MyEntity entity = new MyEntity(1L, new A(2L));
        final DBObject dbObject = getMorphia().toDBObject(entity);
        assertEquals(new BasicDBObject("_id", 1L).append("a", new BasicDBObject("value", 2L)), dbObject);
        assertEquals(entity, getMorphia().fromDBObject(MyEntity.class, dbObject));
    }

    /**
     * This test shows the full serialization, including bson encoding/decoding.
     */
    @Test
    public void testFullBSONSerialization() {
        final MyEntity entity = new MyEntity(1L, new A(2L));
        final DBObject dbObject = getMorphia().toDBObject(entity);

        final byte[] data = new DefaultDBEncoder().encode(dbObject);

        final DBObject decoded = new DefaultDBDecoder().decode(data, (DBCollection) null);
        // fails with a
        // org.mongodb.morphia.mapping.MappingException: No usable
        // constructor
        // for InheritanceTest$A
        final MyEntity actual = getMorphia().fromDBObject(MyEntity.class, decoded);
        assertEquals(entity, actual);
    }

}

