package org.mongodb.morphia.ext.entityscanner;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.utils.Assert;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Provides a scanner to find entities according to a given predicate
 *
 * @author us@thomas-daily.de
 *
 * @deprecated this class is vestigial and incomplete.  no replacement is planned.
 */
@Deprecated
public class EntityScanner {

    /**
     * Creates an EntityScanner using the given Morphia instance with no predicate defined.
     *
     * @param m the Morphia instance
     */
    public EntityScanner(final Morphia m) {
        this(m, null);
    }

    /**
     * Creates an EntityScanner using the given Morphia instance with the given predicate.
     *
     * @param m         the Morphia instance
     * @param predicate the Predicate to use when determining which classes to map.
     */
    public EntityScanner(final Morphia m, final Predicate<String> predicate) {
        Predicate<String> localPredicate = predicate;
        if (localPredicate == null) {
            localPredicate = Predicates.alwaysTrue();
        }
        Assert.parametersNotNull("m, predicate", m, localPredicate);
        final ConfigurationBuilder conf = new ConfigurationBuilder();
        conf.setScanners(new TypeElementsScanner(), new TypeAnnotationsScanner());

        final Set<URL> s = new HashSet<URL>();
        s.addAll(ClasspathHelper.forClassLoader());
        s.addAll(ClasspathHelper.forJavaClassPath());
        final Iterator<URL> iterator = s.iterator();
        while (iterator.hasNext()) {
            final URL url = iterator.next();
            if (url.getPath().endsWith("jnilib")) {
                iterator.remove();
            }
        }
        conf.setUrls(new ArrayList<URL>(s));

        conf.filterInputsBy(localPredicate);
        conf.addScanners(new SubTypesScanner());

        final Reflections r = new Reflections(conf);

        final Set<Class<?>> entities = r.getTypesAnnotatedWith(Entity.class);
        for (final Class<?> c : entities) {
            m.map(c);
        }
    }
}
