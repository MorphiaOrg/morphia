package dev.morphia.experimental;

import com.mongodb.client.ClientSession;
import dev.morphia.AdvancedDatastore;

/**
 * Wraps a ClientSession reference for convenient use of MongoDB's multidocument transaction support.
 *
 * @morphia.experimental
 */
@SuppressWarnings("removal")
public interface MorphiaSession extends AdvancedDatastore, ClientSession {
}
