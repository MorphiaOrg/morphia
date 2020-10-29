package dev.morphia.mapping.validation;

import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.mapping.validation.classrules.DuplicatedAttributeNames;
import dev.morphia.mapping.validation.classrules.EmbeddedAndId;
import dev.morphia.mapping.validation.classrules.EmbeddedAndValue;
import dev.morphia.mapping.validation.classrules.EntityAndEmbed;
import dev.morphia.mapping.validation.classrules.EntityCannotBeMapOrIterable;
import dev.morphia.mapping.validation.classrules.EntityOrEmbed;
import dev.morphia.mapping.validation.classrules.MultipleId;
import dev.morphia.mapping.validation.classrules.MultipleVersions;
import dev.morphia.mapping.validation.classrules.NoId;
import dev.morphia.mapping.validation.fieldrules.ContradictingFieldAnnotation;
import dev.morphia.mapping.validation.fieldrules.LazyReferenceMissingDependencies;
import dev.morphia.mapping.validation.fieldrules.LazyReferenceOnArray;
import dev.morphia.mapping.validation.fieldrules.MapKeyTypeConstraint;
import dev.morphia.mapping.validation.fieldrules.ReferenceToUnidentifiable;
import dev.morphia.mapping.validation.fieldrules.VersionMisuse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;
import static java.util.Collections.sort;

/**
 * Validator for mapped types
 */
public class MappingValidator {

    private static final Logger LOG = LoggerFactory.getLogger(MappingValidator.class);
    private final MorphiaInstanceCreator creator;

    /**
     * Creates a mapping validator
     *
     * @param objectFactory the object factory to be used when creating throw away instances to use in validation
     */
    public MappingValidator(MorphiaInstanceCreator objectFactory) {
        creator = objectFactory;
    }

    /**
     * @param entityModel the EntityModel to validate
     * @param mapper      the Mapper to use for validation
     * @morphia.internal
     */
    public void validate(Mapper mapper, EntityModel entityModel) {
        final Set<ConstraintViolation> ve = new TreeSet<>((o1, o2) -> o1.getLevel().ordinal() > o2.getLevel().ordinal() ? -1 : 1);

        final List<ClassConstraint> rules = getConstraints();
        for (ClassConstraint v : rules) {
            v.check(mapper, entityModel, ve);
        }

        if (!ve.isEmpty()) {
            final ConstraintViolation worst = ve.iterator().next();
            final Level maxLevel = worst.getLevel();
            if (maxLevel.ordinal() >= Level.FATAL.ordinal()) {
                throw new ConstraintViolationException(ve);
            }

            // sort by class to make it more readable
            final List<LogLine> l = new ArrayList<>();
            for (ConstraintViolation v : ve) {
                l.add(new LogLine(v));
            }
            sort(l);

            for (LogLine line : l) {
                line.log(LOG);
            }
        }
    }

    private List<ClassConstraint> getConstraints() {
        final List<ClassConstraint> constraints = new ArrayList<>(32);

        // normally, i do this with scanning the classpath, but that´d bring
        // another dependency ;)

        // class-level
        constraints.add(new MultipleId());
        constraints.add(new MultipleVersions());
        constraints.add(new NoId());
        constraints.add(new EmbeddedAndId());
        constraints.add(new EntityAndEmbed());
        constraints.add(new EntityOrEmbed());
        constraints.add(new EmbeddedAndValue());
        constraints.add(new EntityCannotBeMapOrIterable());
        constraints.add(new DuplicatedAttributeNames());
        constraints.add(new ConstructorParameterNameConstraint());
        // constraints.add(new ContainsEmbeddedWithId());
        // field-level
        constraints.add(new ReferenceToUnidentifiable());
        constraints.add(new LazyReferenceMissingDependencies());
        constraints.add(new LazyReferenceOnArray());
        constraints.add(new MapKeyTypeConstraint());
        constraints.add(new VersionMisuse(creator));

        constraints.add(new ContradictingFieldAnnotation(Reference.class, Property.class));

        return constraints;
    }

    static class LogLine implements Comparable<LogLine> {
        private final ConstraintViolation v;

        LogLine(ConstraintViolation v) {
            this.v = v;
        }

        @Override
        public int compareTo(LogLine o) {
            return v.getPrefix().compareTo(o.v.getPrefix());
        }

        @Override
        public int hashCode() {
            return v.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final LogLine logLine = (LogLine) o;

            return v.equals(logLine.v);

        }

        void log(Logger logger) {
            switch (v.getLevel()) {
                case SEVERE:
                    logger.error(v.render());
                    break;
                case WARNING:
                    logger.warn(v.render());
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
