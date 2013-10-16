package org.mongodb.morphia.aggregation;


public interface Projection<T, U> {
  void suppress();
  
  void targetName(String field);
  
  AggregationOperator<T, U> calculate();

  /**
   * Computes the sum of an array of numbers.
   */
  void add(int... values);

  /**
   * Takes two numbers and divides the first number by the second.
   */
  void divide(int numerator, int divisor);

  /**
   * Takes two numbers and calculates the modulo of the first number divided by the second.
   */
  void mod(int first, int second);

  /**
   * Computes the product of an array of numbers.
   */
  void multiply(int... values);

  /**
   * Takes two numbers and subtracts the second number from the first.
   */
  void subtract(int first, int second);
  
}
