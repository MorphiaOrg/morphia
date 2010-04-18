package com.google.code.morphia;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class Modifiers implements Serializable {

    private final Map<String,Map<String,Object>> operations;

    public Modifiers() {
        this.operations = new HashMap<String,Map<String,Object>>();
    }

    /**
     * Sets field to the value supplied.
     */
    public Modifiers set( String field, Object value ) {
        return addOperation("$set", field, value);
    }

    /**
     * Deletes the given field from an object.
     */
    public Modifiers unset( String field ) {
        return addOperation("$unset", field, 1);
    }

    /**
     * Increments field by the number value if field is present in the object,
     * otherwise sets field to the number value.
     */
    public Modifiers inc( String field, int value ) {
        return addOperation("$inc", field, value);
    }

    /**
     * Appends value to field, if field is an existing array, otherwise sets
     * field to the array [value] if field is not present. If field is present
     * but is not an array, an error condition is raised.
     */
    public Modifiers push( String field, Object value ) {
        return addOperation("$push", field, value);
    }

    /**
     * Appends each value in values to field, if field is an existing array,
     * otherwise sets field to the array values if field is not present.
     * If field is present but is not an array, an error condition is raised.
     */
    public Modifiers pushAll( String field, List values ) {
        return addOperation("$pushAll", field, values);
    }

    /**
     * Adds value to the array only if its not in the array already.
     */
    public Modifiers addToSet( String field, Object value ) {
        return addOperation("$addToSet", field, value);
    }

    /**
     * Adds values to the array only if they're not in the array already.
     */
    public Modifiers addToSet( String field, List values ) {
        return addOperation("$addToSet", field, Constraints.map("$each", values));
    }

    /**
     * Removes the first element in an array.
     */
    public Modifiers popFirst( String field ) {
        return addOperation("$pop", field, -1);
    }

    /**
     * Removes the last element in an array.
     */
    public Modifiers popLast( String field ) {
        return addOperation("$pop", field, 1);
    }

    /**
     * Removes all occurrences of value from field, if field is an array.
     * If field is present but is not an array, an error condition is raised.
     */
    public Modifiers pull( String field, Object value ) {
        return addOperation("$pull", field, value);
    }

    /**
     * Removes all occurrences of each value in value_array from field, if field
     * is an array. If field is present but is not an array, an error condition is raised.
     */
    public Modifiers pullAll( String field, List values ) {
        return addOperation("$pullAll", field, values);
    }

    public Map<String,Map<String,Object>> getOperations() {
        return operations;
    }

    private Modifiers addOperation( String op, String field, Object value ) {
        if ( !operations.containsKey(op) ) {
            operations.put(op, Constraints.map(field, Constraints.valueOf(value)));
        } else {
            operations.get(op).put(field, Constraints.valueOf(value));
        }
        return this;
    }

}
