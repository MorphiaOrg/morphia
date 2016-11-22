package org.mongodb.morphia.mapping.validation;

import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
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
import java.util.List;

/**
 * A default constraint factory including all ClassConstraints.
 *
 * @author Ross M. Lodge
 */
public class DefaultClassConstraintFactory implements ClassConstraintFactory {
    @Override
    public List<ClassConstraint> getConstraints(ObjectFactory creator) {
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
}
