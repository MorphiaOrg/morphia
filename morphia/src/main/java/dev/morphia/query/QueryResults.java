package dev.morphia.query;


/**
 * The results of a query.  These results aren't materialized until a method on this interface is called.
 *
 * @param <T>
 * @deprecated use {@link Query} instead
 */
@Deprecated
public interface QueryResults<T> extends Iterable<T> {

}
