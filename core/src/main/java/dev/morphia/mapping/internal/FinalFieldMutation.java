package dev.morphia.mapping.internal;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;

import dev.morphia.annotations.internal.MorphiaInternal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Reports whether this JVM lets Morphia set final fields reflectively. JEP 500 makes JDK 26 warn the first time it happens
 * and lets {@code --illegal-final-field-mutation=deny} refuse it, which a future JDK will do by default. Rather than read
 * the launcher options, which differ by JDK, Morphia asks the JVM by setting a final field of its own once.
 * <p>
 * The JVM decides per calling module, and the probe calls from Morphia's. Critter's woven writers call from the entity's
 * module instead, so the answer can be wrong for them when only some modules may set finals, e.g. with Morphia in a named
 * module and {@code --enable-final-field-mutation=ALL-UNNAMED}. Enabling Morphia's module as well avoids that (#4507).
 *
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public final class FinalFieldMutation {
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "only written, to probe the JVM")
    private final Object probe = new Object();

    private FinalFieldMutation() {
    }

    /**
     * Probes the JVM on first use. On JDK 26 in its default {@code warn} mode, that first use triggers the JVM's one-time
     * warning, so call this only when an entity would set a final field reflectively anyway.
     *
     * @return true if setting a final field reflectively fails on this JVM
     */
    public static boolean denied() {
        return Probe.DENIED;
    }

    private static final class Probe {
        private static final boolean DENIED = probe();

        private static boolean probe() {
            try {
                Field field = FinalFieldMutation.class.getDeclaredField("probe");
                field.setAccessible(true);
                field.set(new FinalFieldMutation(), new Object());
                return false;
            } catch (IllegalAccessException | InaccessibleObjectException e) {
                return true;
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
