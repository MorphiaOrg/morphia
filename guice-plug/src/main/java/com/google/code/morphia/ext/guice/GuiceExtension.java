/**
 * 
 */
package com.google.code.morphia.ext.guice;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.mapping.MapperOptions;
import com.google.code.morphia.utils.Assert;
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
