package dev.morphia.test.mapping.lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dev.morphia.Datastore;
import dev.morphia.annotations.Reference;
import dev.morphia.test.mapping.ProxyTestBase;
import dev.morphia.test.models.TestEntity;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Each {@code ReferenceCodec} builds its proxy class once, on the first lazy decode. Concurrent first decodes must agree
 * on that class, and every proxy must still get its own handler.
 */
@Tag("references")
public class TestLazyProxyReuse extends ProxyTestBase {
    private static final int THREADS = 8;

    @Test
    public void concurrentFirstDecodesShareOneProxyClass() throws Exception {
        checkForProxyTypes();

        Datastore datastore = getDs();
        Referenced single = new Referenced("single");
        Referenced first = new Referenced("first");
        Referenced second = new Referenced("second");
        datastore.save(List.of(single, first, second));

        Holder holder = new Holder();
        holder.single = single;
        holder.many = List.of(first, second);
        datastore.save(holder);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Holder> decoded = new ArrayList<>();
        try {
            List<Future<Holder>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return datastore.find(Holder.class).filter(eq("_id", holder.getId())).first();
                }));
            }
            start.countDown();
            for (Future<Holder> future : futures) {
                decoded.add(future.get());
            }
        } finally {
            executor.shutdownNow();
        }
        decoded.add(datastore.find(Holder.class).filter(eq("_id", holder.getId())).first());

        Class<?> singleProxy = decoded.get(0).single.getClass();
        Class<?> manyProxy = decoded.get(0).many.getClass();
        for (Holder each : decoded) {
            assertNotNull(each);
            assertIsProxy(each.single);
            assertIsProxy(each.many);
            assertEquals(singleProxy, each.single.getClass());
            assertEquals(manyProxy, each.many.getClass());
        }

        // check each proxy just before resolving it: a handler shared between decodes would already be fetched here
        for (Holder each : decoded) {
            assertNotFetched(each.single);
            assertNotFetched(each.many);
            assertEquals("single", each.single.getName());
            assertEquals(2, each.many.size());
            assertEquals("first", each.many.get(0).getName());
            assertEquals("second", each.many.get(1).getName());
            assertFetched(each.single);
            assertFetched(each.many);
        }
    }

    public static class Holder extends TestEntity {
        @Reference(lazy = true)
        private Referenced single;
        @Reference(lazy = true)
        private List<Referenced> many;
    }

    public static class Referenced extends TestEntity {
        private String name;

        public Referenced() {
        }

        public Referenced(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
