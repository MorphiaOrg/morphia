package org.mongodb.morphia.callbacks;


import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.callbacks.TestSimpleValidationViaInterceptor.NonNullValidation.NonNullValidationException;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;

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
        MappedField.addInterestingAnnotation(NonNull.class);
    }

    @Test
    public void testGlobalEntityInterceptorWorksAfterEntityCallback() {

        getMorphia().getMapper().addInterceptor(new NonNullValidation());
        getMorphia().map(E.class);
        getMorphia().map(E2.class);

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

    static class E2 {
        @Id
        private final ObjectId id = new ObjectId();

        @NonNull
        private String mustFailValidation;
    }

    public static class NonNullValidation extends AbstractEntityInterceptor {
        @Override
        public void prePersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
            final MappedClass mc = mapper.getMappedClass(ent);
            final List<MappedField> fieldsToTest = mc.getFieldsAnnotatedWith(NonNull.class);
            for (final MappedField mf : fieldsToTest) {
                if (mf.getFieldValue(ent) == null) {
                    throw new NonNullValidationException(mf);
                }
            }
        }

        static class NonNullValidationException extends RuntimeException {

            public NonNullValidationException(final MappedField mf) {
                super("NonNull field is null " + mf.getFullName());
            }

        }
    }
}
