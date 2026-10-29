package dev.morphia.mapping.validation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.mapping.validation.classrules.DuplicatedAttributeNames;
import dev.morphia.mapping.validation.classrules.EntityOrExternal;
import dev.morphia.mapping.validation.classrules.MultipleVersions;
import dev.morphia.mapping.validation.classrules.ShardKeyNames;
import dev.morphia.mapping.validation.fieldrules.ContradictingAnnotations;
import dev.morphia.mapping.validation.fieldrules.IdDoesNotMix;
import dev.morphia.mapping.validation.fieldrules.LazyReferenceMissingDependencies;
import dev.morphia.mapping.validation.fieldrules.LazyReferenceOnArray;
import dev.morphia.mapping.validation.fieldrules.MapKeyTypeConstraint;
import dev.morphia.mapping.validation.fieldrules.PropertyConstraint;
import dev.morphia.mapping.validation.fieldrules.ReferenceToUnidentifiable;
import dev.morphia.mapping.validation.fieldrules.VersionMisuse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.Collections.sort;

/**
 * Validator for mapped types
 * 
 * @hidden
 */
public class MappingValidator {

    private static final Logger LOG = LoggerFactory.getLogger(MappingValidator.class);

    // the rules are stateless, so built once. MultipleId isn't here: two @Id properties both map to _id, so mapping
    // fails with a duplicate mapped name before validation runs
    private static final List<ClassConstraint> CLASS_RULES = List.of(
            new MultipleVersions(),
            new EntityOrExternal(),
            new DuplicatedAttributeNames(),
            new ShardKeyNames());

    private static final List<PropertyConstraint> PROPERTY_RULES = List.of(
            new IdDoesNotMix(),
            new ReferenceToUnidentifiable(),
            new LazyReferenceMissingDependencies(),
            new LazyReferenceOnArray(),
            new MapKeyTypeConstraint(),
            new VersionMisuse(),
            new ContradictingAnnotations(Reference.class, Property.class));

    /**
     * @param entityModel the EntityModel to validate
     * @param mapper      the Mapper to use for validation
     * @morphia.internal
     */
    @MorphiaInternal
    public void validate(Mapper mapper, EntityModel entityModel) {
        // violations are distinct objects, so a set in report order keeps them all
        final Set<ConstraintViolation> found = new LinkedHashSet<>();
        for (ClassConstraint rule : CLASS_RULES) {
            rule.check(mapper, entityModel, found);
        }
        // every property rule in one pass over the properties
        for (PropertyModel property : entityModel.getProperties()) {
            for (PropertyConstraint rule : PROPERTY_RULES) {
                rule.checkProperty(mapper, entityModel, property, found);
            }
        }

        if (!found.isEmpty()) {
            // most severe first; the sort is stable, so equal levels stay in the order they were found
            final List<ConstraintViolation> ve = new ArrayList<>(found);
            ve.sort(Comparator.comparing(ConstraintViolation::getLevel).reversed());
            final Level maxLevel = ve.get(0).getLevel();
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
                line.log();
            }
        }
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

        void log() {
            switch (v.getLevel()) {
                case SEVERE:
                    LOG.error(v.render());
                    break;
                case WARNING:
                    LOG.warn(v.render());
                    break;
                case INFO:
                    LOG.info(v.render());
                    break;
                case MINOR:
                    LOG.debug(v.render());
                    break;
                default:
                    throw new IllegalStateException(format("Cannot log %s of Level %s", ConstraintViolation.class.getSimpleName(),
                            v.getLevel()));
            }
        }
    }
}
