package org.mongodb.morphia.mapping.validation;

import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;
import org.mongodb.morphia.mapping.validation.classrules.DuplicatedAttributeNames;
import org.mongodb.morphia.mapping.validation.classrules.EmbeddedAndId;
import org.mongodb.morphia.mapping.validation.classrules.EmbeddedAndValue;
import org.mongodb.morphia.mapping.validation.classrules.EntityAndEmbed;
import org.mongodb.morphia.mapping.validation.classrules.EntityCannotBeMapOrIterable;
import org.mongodb.morphia.mapping.validation.classrules.MultipleId;
import org.mongodb.morphia.mapping.validation.classrules.MultipleVersions;
import org.mongodb.morphia.mapping.validation.classrules.NoId;
import org.mongodb.morphia.mapping.validation.fieldrules.ContradictingFieldAnnotation;
import org.mongodb.morphia.mapping.validation.fieldrules.LazyReferenceMissingDependencies;
import org.mongodb.morphia.mapping.validation.fieldrules.LazyReferenceOnArray;
import org.mongodb.morphia.mapping.validation.fieldrules.MapKeyDifferentFromString;
import org.mongodb.morphia.mapping.validation.fieldrules.MapNotSerializable;
import org.mongodb.morphia.mapping.validation.fieldrules.MisplacedProperty;
import org.mongodb.morphia.mapping.validation.fieldrules.ReferenceToUnidentifiable;
import org.mongodb.morphia.mapping.validation.fieldrules.VersionMisuse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MappingValidator {

    private static final Logger LOG = MorphiaLoggerFactory.get(MappingValidator.class);
    private ObjectFactory creator;

    public MappingValidator(final ObjectFactory objectFactory) {
        creator = objectFactory;
    }

    public void validate(final List<MappedClass> classes) {
        final Set<ConstraintViolation> ve = new TreeSet<ConstraintViolation>(new Comparator<ConstraintViolation>() {

            public int compare(final ConstraintViolation o1, final ConstraintViolation o2) {
                return o1.getLevel().ordinal() > o2.getLevel().ordinal() ? -1 : 1;
            }
        });

        final List<ClassConstraint> rules = getConstraints();
        for (final MappedClass c : classes) {
            for (final ClassConstraint v : rules) {
                v.check(c, ve);
            }
        }

        if (!ve.isEmpty()) {
            final ConstraintViolation worst = ve.iterator().next();
            final Level maxLevel = worst.getLevel();
            if (maxLevel.ordinal() >= Level.FATAL.ordinal()) {
                throw new ConstraintViolationException(ve);
            }

            // sort by class to make it more readable
            final List<LogLine> l = new ArrayList<LogLine>();
            for (final ConstraintViolation v : ve) {
                l.add(new LogLine(v));
            }
            Collections.sort(l);

            for (final LogLine line : l) {
                line.log(LOG);
            }
        }
    }

    private List<ClassConstraint> getConstraints() {
        final List<ClassConstraint> constraints = new ArrayList<ClassConstraint>(32);

        // normally, i do this with scanning the classpath, but that´d bring
        // another dependency ;)

        // class-level
        constraints.add(new MultipleId());
        constraints.add(new MultipleVersions());
        constraints.add(new NoId());
        constraints.add(new EmbeddedAndId());
        constraints.add(new EntityAndEmbed());
        constraints.add(new EmbeddedAndValue());
        constraints.add(new EntityCannotBeMapOrIterable());
        constraints.add(new DuplicatedAttributeNames());
        // constraints.add(new ContainsEmbeddedWithId());
        // field-level
        constraints.add(new MisplacedProperty());
        constraints.add(new ReferenceToUnidentifiable());
        constraints.add(new LazyReferenceMissingDependencies());
        constraints.add(new LazyReferenceOnArray());
        constraints.add(new MapKeyDifferentFromString());
        constraints.add(new MapNotSerializable());
        constraints.add(new VersionMisuse(creator));
        //
        constraints.add(new ContradictingFieldAnnotation(Reference.class, Serialized.class));
        constraints.add(new ContradictingFieldAnnotation(Reference.class, Property.class));
        constraints.add(new ContradictingFieldAnnotation(Reference.class, Embedded.class));
        //
        constraints.add(new ContradictingFieldAnnotation(Embedded.class, Serialized.class));
        constraints.add(new ContradictingFieldAnnotation(Embedded.class, Property.class));
        //
        constraints.add(new ContradictingFieldAnnotation(Property.class, Serialized.class));

        return constraints;
    }

    static class LogLine implements Comparable<LogLine> {
        private final ConstraintViolation v;

        LogLine(final ConstraintViolation v) {
            this.v = v;
        }

        void log(final Logger logger) {
            switch (v.getLevel()) {
                case SEVERE:
                    logger.error(v.render());
                    break;
                case WARNING:
                    logger.warning(v.render());
                    break;
                case INFO:
                    logger.info(v.render());
                    break;
                case MINOR:
                    logger.debug(v.render());
                    break;
                default:
                    throw new IllegalStateException(format("Cannot log %s of Level %s", ConstraintViolation.class.getSimpleName(),
                                                           v.getLevel()));
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final LogLine logLine = (LogLine) o;

            return v.equals(logLine.v);

        }

        @Override
        public int hashCode() {
            return v.hashCode();
        }

        public int compareTo(final LogLine o) {
            return v.getPrefix().compareTo(o.v.getPrefix());
        }
    }

    /**
     * i definitely vote for all at once validation
     */
    @Deprecated
    public void validate(final MappedClass mappedClass) {
        validate(Arrays.asList(mappedClass));
    }
}
