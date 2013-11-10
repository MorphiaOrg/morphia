package org.mongodb.morphia.aggregation;


public interface AggregationOperator<T, U> {
  /**
   * Computes the sum of an array of numbers.
   */
  AggregationPipeline<T, U> add(int... values);

  /**
   * Takes two numbers and divides the first number by the second.
   */
  AggregationPipeline<T, U> divide(int numerator, int divisor);

  /**
   * Takes two numbers and calculates the modulo of the first number divided by the second.
   */
  AggregationPipeline<T, U> mod(int first, int second);

  /**
   * Computes the product of an array of numbers.
   */
  AggregationPipeline<T, U> multiply(int... values);

  /**
   * Takes two numbers and subtracts the second number from the first.
   */
  AggregationPipeline<T, U> subtract(int first, int second);
}
