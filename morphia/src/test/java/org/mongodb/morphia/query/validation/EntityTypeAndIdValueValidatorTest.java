package org.mongodb.morphia.query.validation;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.entities.EntityWithNoId;
import org.mongodb.morphia.entities.SimpleEntity;
import org.mongodb.morphia.mapping.Mapper;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EntityTypeAndIdValueValidatorTest {
    @Test
    public void shouldAllowTypeThatIsAMappedEntityAndAValueWithSameClassAsIdOfMappedEntity() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = EntityTypeAndIdValueValidator.getInstance().apply(new Mapper(),
                                                                                      SimpleEntity.class,
                                                                                      new ObjectId(),
                                                                                      validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectValueWithATypeThatDoesNotMatchTheEntityIdFieldType() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = EntityTypeAndIdValueValidator.getInstance().apply(new Mapper(),
                                                                                      SimpleEntity.class,
                                                                                      "some non-ObjectId value",
                                                                                      validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
    }

    @Test
    public void shouldNotValidateIfEntityHasNoIdField() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = EntityTypeAndIdValueValidator.getInstance().apply(new Mapper(),
                                                                                      EntityWithNoId.class,
                                                                                      "some non-null value",
                                                                                      validationFailures);
        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }
}