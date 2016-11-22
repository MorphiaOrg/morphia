package org.mongodb.morphia.mapping.validation;

import org.mongodb.morphia.ObjectFactory;

import java.util.List;

/**
 * A factory for constructing validation constraints.
 *
 * @author Ross M. Lodge
 */
public interface ClassConstraintFactory {

    List<ClassConstraint> getConstraints(ObjectFactory creator);

}
