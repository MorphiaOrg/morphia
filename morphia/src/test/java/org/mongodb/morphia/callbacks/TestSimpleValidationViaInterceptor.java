package org.mongodb.morphia.callbacks;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.List;

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
import com.mongodb.DBObject;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class TestSimpleValidationViaInterceptor extends TestBase {

  static class E {
    @Id
    private final ObjectId _id = new ObjectId();

    @NonNull Date lastModified;

    @PrePersist void entityCallback() {
      lastModified = new Date();
    }
  }

  static class E2 {
    @Id
    private final ObjectId _id = new ObjectId();

    @NonNull String mustFailValidation;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD })
  public @interface NonNull {
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

  static {
    MappedField.interestingAnnotations.add(NonNull.class);
  }

  @Test
  public void testGlobalEntityInterceptorWorksAfterEntityCallback() {

    morphia.getMapper().addInterceptor(new NonNullValidation());
    morphia.map(E.class);
    morphia.map(E2.class);

    ds.save(new E());
    try {
      ds.save(new E2());
      Assert.fail();
    } catch (NonNullValidationException e) {
      // expected
    }

  }
}
