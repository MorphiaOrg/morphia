package xyz.morphia.ext.entityscanner;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.bson.types.ObjectId;
import org.junit.Test;
import xyz.morphia.Morphia;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author us@thomas-daily.de
 */
@SuppressWarnings("deprecation")
public class EntityScannerTest {
    @Test
    public void testScanning() throws Exception {
        final Morphia m = new Morphia();
        assertFalse(m.isMapped(E.class));
        new EntityScanner(m, Predicates.equalTo(E.class.getName() + ".class"));
        assertTrue(m.isMapped(E.class));
        assertFalse(m.isMapped(F.class));
        new EntityScanner(m, new Predicate<String>() {

            @Override
            public boolean apply(final String input) {
                return input.startsWith(EntityScannerTest.class.getPackage().getName());
            }
        });
        assertTrue(m.isMapped(F.class));

    }

    @Entity
    private static class E {
        @Id
        private ObjectId id;
    }

    @Entity
    private static class F {
        @Id
        private ObjectId id;
    }
}
