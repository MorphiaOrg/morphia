package org.mongodb.morphia.ext.entityscanner;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.utils.Assert;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.scannotation.ClasspathUrlFinder;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author us@thomas-daily.de
 */
public class EntityScanner {

  public EntityScanner(final Morphia m) {
    this(m, null);

  }

  public EntityScanner(final Morphia m, final Predicate<String> predicate) {
    Predicate<String> localPredicate = predicate;
    if (localPredicate == null) {
      localPredicate = Predicates.alwaysTrue();
    }
    Assert.parametersNotNull("m, predicate", m, localPredicate);
    final ConfigurationBuilder conf = new ConfigurationBuilder();
    conf.setScanners(new TypesScanner(), new TypeAnnotationsScanner());

    final Set<URL> s = new HashSet<URL>();
    s.addAll(ClasspathHelper.forClassLoader());
    s.addAll(Arrays.asList(ClasspathUrlFinder.findClassPaths()));
    final Iterator<URL> iterator = s.iterator();
    while (iterator.hasNext()) {
      final URL url = iterator.next();
      if (url.getPath().endsWith("jnilib")) {
        iterator.remove();
      }
    }
    conf.setUrls(new ArrayList<URL>(s));

    conf.filterInputsBy(localPredicate);

    final Reflections r = new Reflections(conf);

    final Set<Class<?>> entities = r.getTypesAnnotatedWith(Entity.class);
    for (final Class<?> c : entities) {
      m.map(c);
    }
  }

}