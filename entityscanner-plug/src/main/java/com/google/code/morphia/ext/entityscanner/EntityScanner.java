/**
 * 
 */
package com.google.code.morphia.ext.entityscanner;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.scannotation.ClasspathUrlFinder;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.utils.Assert;
import com.google.common.base.Predicates;

/**
 * @author us@thomas-daily.de
 * 
 */
public class EntityScanner {
	
	public EntityScanner(final Morphia m) {
		this(m, null);
		
	}
	
	public EntityScanner(final Morphia m, com.google.common.base.Predicate<String> predicate) {
		if (predicate == null) {
			predicate = Predicates.alwaysTrue();
		}
		Assert.parametersNotNull("m, predicate", m, predicate);
		final ConfigurationBuilder conf = new ConfigurationBuilder();
		conf.setScanners(new TypesScanner(), new TypeAnnotationsScanner());
		
		final Set<URL> s = new HashSet<URL>();
		s.addAll(ClasspathHelper.getUrlsForCurrentClasspath());
		s.addAll(Arrays.asList(ClasspathUrlFinder.findClassPaths()));
		conf.setUrls(new ArrayList(s));
		
		conf.filterInputsBy(predicate);
		
		final Reflections r = new Reflections(conf);
		
		final Set<Class<?>> entities = r.getTypesAnnotatedWith(Entity.class);
		for (final Class<?> c : entities) {
			m.map(c);
		}
	}
	
}