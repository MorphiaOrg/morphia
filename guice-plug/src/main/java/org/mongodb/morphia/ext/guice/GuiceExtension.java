package org.mongodb.morphia.ext.guice;


import com.google.inject.Injector;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MapperOptions;
import org.mongodb.morphia.utils.Assert;


/**
 * @author us@thomas-daily.de
 */
public class GuiceExtension {

    /**
     * Creates a GuiceExtension to configure Morphia to use Guice
     *
     * @param morphia  the Morphia instance to update
     * @param injector the Guice Injector to use when instantiating Entities
     */
    public GuiceExtension(final Morphia morphia, final Injector injector) {
        Assert.parameterNotNull("morphia", morphia);
        final MapperOptions options = morphia.getMapper()
                                             .getOptions();
        options.setObjectFactory(new GuiceObjectFactory(options.getObjectFactory(), injector));
    }
}
