package org.mongodb.morphia.ext.guice;

import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MapperOptions;
import org.mongodb.morphia.utils.Assert;
import com.google.inject.Injector;

/**
 * @author us@thomas-daily.de
 */
public class GuiceExtension {

	public GuiceExtension(final Morphia morphia, final Injector injector) {
		Assert.parameterNotNull(morphia, "morphia");
		final MapperOptions options = morphia.getMapper().getOptions();
		options.objectFactory = new GuiceObjectFactory(options.objectFactory, injector);
	}
}
