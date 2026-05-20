package dev.morphia.test.mapping.codec;

import java.util.BitSet;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.List.of;

public class BitSetCodecTest extends TestBase {
    public BitSetCodecTest() {
        super(buildConfig()
                .packages(of("dev.morphia.test.mapping.codec")));
    }

    @Entity(value = "ClassWithBitSet")
    public static class ClassWithBitSet {
        public @Id Long id;
        public BitSet bits;

        ClassWithBitSet() {
        }

        ClassWithBitSet(long id, BitSet bits) {
            this.id = id;
            this.bits = bits;
        }
    }

    @Test
    public void testBitSetBasicUsage() {
        ClassWithBitSet testClass = new ClassWithBitSet(1L, new BitSet());
        testClass.bits.set(4);
        testClass.bits.set(44);
        getDs().save(testClass);
        getMapper().map(ClassWithBitSet.class);

        ClassWithBitSet found = getDs().find(ClassWithBitSet.class).filter(Filters.eq("id", testClass.id)).first();
        Assertions.assertEquals(false, found.bits.get(3));
        Assertions.assertEquals(true, found.bits.get(4));
        Assertions.assertEquals(false, found.bits.get(5));
        Assertions.assertEquals(false, found.bits.get(43));
        Assertions.assertEquals(true, found.bits.get(44));
        Assertions.assertEquals(false, found.bits.get(45));
    }

    @Test
    public void testBitSetEmptyCase() {
        ClassWithBitSet testClass = new ClassWithBitSet(1, new BitSet());
        getDs().save(testClass);
        getMapper().map(ClassWithBitSet.class);

        ClassWithBitSet found = getDs().find(ClassWithBitSet.class).filter(Filters.eq("id", testClass.id)).first();
        for (int i = 0; i < found.bits.size(); ++i) {
            Assertions.assertEquals(false, found.bits.get(i));
        }
    }

    @Test
    public void testBitSetNullCase() {
        ClassWithBitSet testClass = new ClassWithBitSet(1, null);
        getDs().save(testClass);
        getMapper().map(ClassWithBitSet.class);

        ClassWithBitSet found = getDs().find(ClassWithBitSet.class).filter(Filters.eq("id", testClass.id)).first();
        Assertions.assertNotNull(found);
        Assertions.assertNull(found.bits);
    }

    @Test
    public void testBitSetDecodeLegacyFormat() {
        // create a Document that describe a ClassWithBitSet object with a legacy BitSet in it
        String bitSetInLegacyFormat = "{\"words\": [{\"$numberLong\": \"16\"}, {\"$numberLong\": \"-3\"}], \"wordsInUse\": {\"$numberInt\": \"2\"}, \"sizeIsSticky\":false}";
        String containingObject = "{\"_id\":{\"$numberLong\":\"2\"}, \"_t\":\"" + ClassWithBitSet.class.getName() + "\", \"bits\":"
                + bitSetInLegacyFormat + "}";

        // insert our legacy-style object
        insert("ClassWithBitSet", of(Document.parse(containingObject)));

        // retrieve document, make sure it's still the same
        ClassWithBitSet result = getDs().find(ClassWithBitSet.class).filter(Filters.eq("_id", 2L)).first();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(false, result.bits.get(3));
        Assertions.assertEquals(true, result.bits.get(4));
        Assertions.assertEquals(false, result.bits.get(5));
        Assertions.assertEquals(true, result.bits.get(64));
        Assertions.assertEquals(false, result.bits.get(65));
        Assertions.assertEquals(true, result.bits.get(66));
    }

}
