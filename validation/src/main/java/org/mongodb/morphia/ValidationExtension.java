package org.mongodb.morphia;


import java.util.Set;
import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.mongodb.morphia.mapping.Mapper;
import com.mongodb.DBObject;


/**
 * @author us@thomas-daily.de
 */
public class ValidationExtension extends AbstractEntityInterceptor {
    private ValidatorFactory validationFactory;

    /**
     * @deprecated use the new ValidationExtension(morphia) convention
     */
    public ValidationExtension() {
    }

    public ValidationExtension(final Morphia m) {
        final Configuration<?> configuration = Validation.byDefaultProvider().configure();
        this.validationFactory = configuration.buildValidatorFactory();

        m.getMapper().addInterceptor(this);
    }

    @Override
    public void prePersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
        final Set validate = this.validationFactory.getValidator().validate(ent);
        if (!validate.isEmpty()) {
            throw new VerboseJSR303ConstraintViolationException(validate);
        }
    }
}
