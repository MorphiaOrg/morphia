package dev.morphia.ext;

import dev.morphia.EntityInterceptor;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.query.FindOptions;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;

public class NewAnnotationTest extends TestBase {

    @Test
    public void testIt() {
        getMapper().addInterceptor(new ToLowercaseHelper());
        getMapper().map(User.class);
        final User u = new User();
        u.email = "ScottHernandez@gmail.com";

        getDs().save(u);

        final User uScott = getDs().find(User.class)
                                   .disableValidation()
                                   .filter(eq("email_lowercase", u.email.toLowerCase())).iterator(new FindOptions()
                                                                                                      .logQuery()
                                                                                                      .limit(1))
                                   .next();
        Assert.assertNotNull(uScott);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface Lowercase {
    }

    @Entity
    private static class User {
        @Id
        private String id;
        @Lowercase
        private String email;
    }

    private static class ToLowercaseHelper implements EntityInterceptor {
        @Override
        public void postLoad(Object ent, Document document, Mapper mapper) {
        }

        @Override
        public void postPersist(Object ent, Document document, Mapper mapper) {
        }

        @Override
        public void preLoad(Object ent, Document document, Mapper mapper) {
        }

        @Override
        public void prePersist(Object ent, Document document, Mapper mapper) {
            final List<PropertyModel> toLowercase = mapper.getEntityModel(ent.getClass()).getProperties(Lowercase.class);
            for (PropertyModel mf : toLowercase) {
                try {
                    final Object fieldValue = mf.getValue(ent);
                    document.put(mf.getMappedName() + "_lowercase", fieldValue.toString().toLowerCase());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
