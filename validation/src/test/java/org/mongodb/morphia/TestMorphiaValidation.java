package org.mongodb.morphia;

import java.lang.Object;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.Email;
import org.junit.Test;

import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.Mapper;
import com.mongodb.DBObject;


/**
 * @author doc
 */
public class TestMorphiaValidation extends TestBase
{

    public static class E
    {
        @Id
        ObjectId id;
        @Email
        String email;
    }

    /**
     * Test method for {@link ValidationExtension#prePersist(Object, DBObject, Mapper)}.
     */
    @Test
    public final void testPrePersist()
    {
        final E e = new E();
        e.email = "not an email";

        new ValidationExtension(this.morphia);

        new AssertedFailure()
        {

            @Override
            protected void thisMustFail() throws Throwable
            {
                TestMorphiaValidation.this.ds.save(e);
            }
        };

        e.email = "foo@bar.com";
        this.ds.save(e);

    }

}
