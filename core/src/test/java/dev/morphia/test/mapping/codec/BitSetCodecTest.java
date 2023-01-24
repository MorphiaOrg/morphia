package dev.morphia.test.mapping.codec;

import java.util.BitSet;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class BitSetCodecTest extends TestBase {

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
        assertEquals(found.bits.get(3), false);
        assertEquals(found.bits.get(4), true);
        assertEquals(found.bits.get(5), false);
        assertEquals(found.bits.get(43), false);
        assertEquals(found.bits.get(44), true);
        assertEquals(found.bits.get(45), false);
    }

    @Test
    public void testBitSetEmptyCase() {
        ClassWithBitSet testClass = new ClassWithBitSet(1, new BitSet());
        getDs().save(testClass);
        getMapper().map(ClassWithBitSet.class);

        ClassWithBitSet found = getDs().find(ClassWithBitSet.class).filter(Filters.eq("id", testClass.id)).first();
        for (int i = 0; i < found.bits.size(); ++i) {
            assertEquals(found.bits.get(i), false);
        }
    }

    @Test
    public void testBitSetNullCase() {
        ClassWithBitSet testClass = new ClassWithBitSet(1, null);
        getDs().save(testClass);
        getMapper().map(ClassWithBitSet.class);

        ClassWithBitSet found = getDs().find(ClassWithBitSet.class).filter(Filters.eq("id", testClass.id)).first();
        assertNotNull(found);
        assertNull(found.bits);
    }

    @Test
    public void testBitSetDecodeLegacyFormat() {
        // create a Document that describe a ClassWithBitSet object with a legacy BitSet in it
        String bitSetInLegacyFormat = "{\"words\": [{\"$numberLong\": \"16\"}, {\"$numberLong\": \"-3\"}], \"wordsInUse\": {\"$numberInt\": \"2\"}, \"sizeIsSticky\":false}";
        String containingObject = "{\"_id\":{\"$numberLong\":\"2\"}, \"_t\":\"ClassWithBitSet\", \"bits\":" + bitSetInLegacyFormat + "}";

        // insert our legacy-style object
        insert("ClassWithBitSet", List.of(Document.parse(containingObject)));

        // retrieve document, make sure it's still the same
        getMapper().map(ClassWithBitSet.class);
        ClassWithBitSet result = getDs().find(ClassWithBitSet.class).filter(Filters.eq("_id", 2L)).first();
        assertNotNull(result);
        assertEquals(result.bits.get(3), false);
        assertEquals(result.bits.get(4), true);
        assertEquals(result.bits.get(5), false);
        assertEquals(result.bits.get(64), true);
        assertEquals(result.bits.get(65), false);
        assertEquals(result.bits.get(66), true);
    }

}
