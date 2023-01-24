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

public class BitSetCodecTest extends TestBase {

    @Entity(value = "ClassWithBitSet")
    public static class ClassWithBitSet {
        public @Id Long id = Long.valueOf(1);
        public BitSet bits = new BitSet();
    }

    @Test
    public void testBitSetBasicUsage() {
        ClassWithBitSet testClass = new ClassWithBitSet();
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
        ClassWithBitSet testClass = new ClassWithBitSet();
        getDs().save(testClass);
        getMapper().map(ClassWithBitSet.class);

        ClassWithBitSet found = getDs().find(ClassWithBitSet.class).filter(Filters.eq("id", testClass.id)).first();
        for (int i = 0; i < found.bits.size(); ++i) {
            assertEquals(found.bits.get(i), false);
        }
    }

    @Test
    public void testBitSetDecodeLegacyFormat() {
        String bitSetInLegacyFormat = "{\"words\": [{\"$numberLong\": \"16\"}, {\"$numberLong\": \"-3\"}], \"wordsInUse\": {\"$numberInt\": \"2\"}, \"sizeIsSticky\":false}";
        String containingObject = "{\"_id\":{\"$numberLong\":\"2\"}, \"_t\":\"ClassWithBitSet\", \"bits\":" + bitSetInLegacyFormat + "}";

        // insert our legacy-formatted document
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
