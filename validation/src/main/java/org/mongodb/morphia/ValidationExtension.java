package org.mongodb.morphia;


import com.mongodb.DBObject;
import org.mongodb.morphia.mapping.Mapper;

import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.Set;


/**
 * @author us@thomas-daily.de
 */
public class ValidationExtension extends AbstractEntityInterceptor {
    private ValidatorFactory validationFactory;

    /**
     * Creates a ValidationExtension
     *
     * @param m the Morphia instance to use
     */
    public ValidationExtension(final Morphia m) {
        final Configuration<?> configuration = Validation.byDefaultProvider().configure();
        validationFactory = configuration.buildValidatorFactory();

        m.getMapper().addInterceptor(this);
    }

    /**
     * @return the ValidatorFactory
     */
    public ValidatorFactory getValidatorFactory() {
        return this.validationFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void prePersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
        final Set validate = validationFactory.getValidator().validate(ent);
        if (!validate.isEmpty()) {
            throw new VerboseJSR303ConstraintViolationException(validate);
        }
    }
}
