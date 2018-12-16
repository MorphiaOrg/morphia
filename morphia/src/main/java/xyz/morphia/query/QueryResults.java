package xyz.morphia.query;


import com.mongodb.Bytes;
import com.mongodb.client.MongoCursor;
import xyz.morphia.Key;

import java.util.List;

/**
 * The results of a query.  These results aren't materialized until a method on this interface is called.
 *
 * @param <T>
 */
public interface QueryResults<T> extends Iterable<T> {

}
