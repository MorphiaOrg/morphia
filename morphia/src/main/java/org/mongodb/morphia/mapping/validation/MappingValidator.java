package org.mongodb.morphia.mapping.validation;

import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.*;
import static java.util.Collections.*;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MappingValidator {

    private static final Logger LOG = MorphiaLoggerFactory.get(MappingValidator.class);
    private ObjectFactory creator;

    /**
     * Creates a mapping validator
     *
     * @param objectFactory the object factory to be used when creating throw away instances to use in validation
     */
    public MappingValidator(final ObjectFactory objectFactory) {
        creator = objectFactory;
    }

    /**
     * Validates a MappedClass
     *
     * @param mappedClass the MappedClass to validate
     * @param mapper the Mapper to use for validation
     */
    @Deprecated
    public void validate(final Mapper mapper, final MappedClass mappedClass) {
        validate(mapper, singletonList(mappedClass));
    }

    /**
     * Validates a List of MappedClasses
     *
     * @param classes the MappedClasses to validate
     * @param mapper the Mapper to use for validation
     */
    public void validate(final Mapper mapper, final List<MappedClass> classes) {
        final Set<ConstraintViolation> ve = new TreeSet<ConstraintViolation>(new Comparator<ConstraintViolation>() {

            @Override
            public int compare(final ConstraintViolation o1, final ConstraintViolation o2) {
                return o1.getLevel().ordinal() > o2.getLevel().ordinal() ? -1 : 1;
            }
        });

        final List<ClassConstraint> rules = mapper.getOptions().getConstraintFactory().getConstraints(creator);
        for (final MappedClass c : classes) {
            for (final ClassConstraint v : rules) {
                v.check(mapper, c, ve);
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
            sort(l);

            for (final LogLine line : l) {
                line.log(LOG);
            }
        }
    }

    static class LogLine implements Comparable<LogLine> {
        private final ConstraintViolation v;

        LogLine(final ConstraintViolation v) {
            this.v = v;
        }

        @Override
        public int compareTo(final LogLine o) {
            return v.getPrefix().compareTo(o.v.getPrefix());
        }

        @Override
        public int hashCode() {
            return v.hashCode();
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
    }
}
