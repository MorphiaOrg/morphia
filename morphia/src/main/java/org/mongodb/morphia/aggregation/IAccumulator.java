package org.mongodb.morphia.aggregation;

/**
 * This interface defines the contract of the accumulator
 * 
 * @author Lukas Zaruba, lukas.zaruba@gmail.com
 *
 */
public interface IAccumulator {

    Object getValue();
    String getOperation();

}
