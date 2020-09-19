package dev.morphia.callbacks;


import dev.morphia.EntityInterceptor;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.callbacks.TestSimpleValidationViaInterceptor.NonNullValidation.NonNullValidationException;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.List;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class TestSimpleValidationViaInterceptor extends TestBase {

    static {
//        MappedField.addInterestingAnnotation(NonNull.class);
    }

    @Test
    public void testGlobalEntityInterceptorWorksAfterEntityCallback() {

        getMapper().addInterceptor(new NonNullValidation());
        getMapper().map(E.class);
        getMapper().map(E2.class);

        getDs().save(new E());
        try {
            getDs().save(new E2());
            Assert.fail();
        } catch (NonNullValidationException e) {
            // expected
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface NonNull {
    }

    @Entity
    static class E {
        @Id
        private final ObjectId id = new ObjectId();

        @NonNull
        private Date lastModified;

        @PrePersist
        void entityCallback() {
            lastModified = new Date();
        }
    }

    @Entity
    static class E2 {
        @Id
        private final ObjectId id = new ObjectId();

        @NonNull
        private String mustFailValidation;
    }

    public static class NonNullValidation implements EntityInterceptor {
        @Override
        public void prePersist(Object ent, Document document, Mapper mapper) {
            final MappedClass mc = mapper.getMappedClass(ent.getClass());
            final List<MappedField> fieldsToTest = mc.getFields(NonNull.class);
            for (MappedField mf : fieldsToTest) {
                if (mf.getFieldValue(ent) == null) {
                    throw new NonNullValidationException(mf);
                }
            }
        }

        static class NonNullValidationException extends RuntimeException {

            NonNullValidationException(MappedField mf) {
                super("NonNull field is null " + mf.getFullName());
            }

        }
    }
}
