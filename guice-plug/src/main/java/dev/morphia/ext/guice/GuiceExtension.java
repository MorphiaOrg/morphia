package dev.morphia.ext.guice;


import com.google.inject.Injector;
import dev.morphia.Morphia;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.utils.Assert;


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
    public GuiceExtension(final Mapper mapper, final Injector injector) {
        Assert.parameterNotNull("mapper", mapper);
        final MapperOptions options = mapper.getOptions();
        options.setObjectFactory(new GuiceObjectFactory(options.getObjectFactory(), injector));
    }
}
