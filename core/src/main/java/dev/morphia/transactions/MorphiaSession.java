package dev.morphia.transactions;

import com.mongodb.client.ClientSession;

import dev.morphia.AdvancedDatastore;

/**
 * Wraps a ClientSession reference for convenient use of MongoDB's multidocument transaction support.
 */
@SuppressWarnings("removal")
public interface MorphiaSession extends AdvancedDatastore, ClientSession {
}
