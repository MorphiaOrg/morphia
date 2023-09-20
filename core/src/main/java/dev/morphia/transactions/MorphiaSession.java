package dev.morphia.transactions;

import com.mongodb.client.ClientSession;

import dev.morphia.Datastore;

/**
 * Wraps a ClientSession reference for convenient use of MongoDB's multidocument transaction support.
 */
public interface MorphiaSession extends Datastore, ClientSession {
}
