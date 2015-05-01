package org.mongodb.morphia.example;

import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;

import java.net.UnknownHostException;

public final class QuickTour {
    private QuickTour() {
    }

    public static void main(final String[] args) throws UnknownHostException {
        final Morphia morphia = new Morphia();

        // tell morphia where to find your classes
        // can be called multiple times with different packages or classes
        morphia.mapPackage("org.mongodb.morphia.example");

        // create the Datastore connecting to the database running on the default port on the local host
        morphia.createDatastore(new MongoClient(), "morphia_example");
    }
}
