package dev.morphia.test.query.proxies;

import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.test.query.TestQueryDiscriminators.Animal;

/**
 * Stands in for a lazy reference's runtime class. Kept out of {@code dev.morphia.test.query} so the package scan there
 * doesn't map it.
 */
public abstract class ProxiedAnimal extends Animal implements MorphiaProxy {
}
