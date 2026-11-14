package dev.morphia.test;

import java.util.List;
import java.util.Set;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;

import dev.morphia.BulkWriteFailure;
import dev.morphia.MorphiaBulkWriteException;
import dev.morphia.VersionMismatchException;
import dev.morphia.internal.BulkWriteFailures;

import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBulkWriteFailures {
    private static MongoBulkWriteException driverException(int inserted, int... errorIndexes) {
        List<BulkWriteError> errors = java.util.Arrays.stream(errorIndexes)
                .mapToObj(i -> new BulkWriteError(11000, "duplicate key", new BsonDocument(), i))
                .toList();
        return new MongoBulkWriteException(BulkWriteResult.acknowledged(inserted, 0, 0, 0, List.of(), List.of()),
                errors, null, new ServerAddress(), Set.of());
    }

    @Test
    public void recordBulkFailureMapsIndexesToCallerList() {
        Thing b = new Thing("b");
        Thing d = new Thing("d");
        BulkWriteFailures failures = new BulkWriteFailures(4);
        failures.recordBulkFailure(driverException(0, 1), List.of(b, d), new int[] { 1, 3 });

        MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, failures::throwIfFailed);
        assertEquals(1, e.getFailures().size());
        assertSame(d, e.getFailures().get(0).entity());
        assertEquals(3, e.getFailures().get(0).index());
        assertEquals(3, e.getWriteErrors().get(0).getIndex());
    }

    @Test
    public void failuresSortedAndUnprocessedSeparate() {
        Thing e2 = new Thing("2");
        Thing e5 = new Thing("5");
        Thing e6 = new Thing("6");
        Thing e7 = new Thing("7");
        BulkWriteFailures failures = new BulkWriteFailures(8);
        failures.recordBulkFailure(driverException(0, 0), List.of(e5), new int[] { 5 });
        failures.recordUnprocessed(e6, 6);
        failures.recordUnprocessed(e7, 7);
        failures.recordBulkFailure(driverException(0, 0), List.of(e2), new int[] { 2 });

        assertEquals(java.util.BitSet.valueOf(new long[] { 0b11100100 }), failures.notStored());
        MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, failures::throwIfFailed);
        assertEquals(List.of(e2, e5), e.getFailedEntities());
        assertEquals(List.of(e6, e7), e.getUnprocessedEntities());
        assertEquals(List.of(2, 5), e.getWriteErrors().stream().map(BulkWriteError::getIndex).toList());
    }

    @Test
    public void versionConflictHasNoWriteError() {
        Thing thing = new Thing("v");
        VersionMismatchException conflict = new VersionMismatchException(Thing.class, "v");
        BulkWriteFailures failures = new BulkWriteFailures(5);
        failures.recordException(thing, 4, conflict);

        MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, failures::throwIfFailed);
        BulkWriteFailure failure = e.getFailures().get(0);
        assertSame(conflict, failure.exception());
        assertNull(failure.error());
        assertTrue(e.getWriteErrors().isEmpty());
        assertSame(conflict, e.getCause());
    }

    @Test
    public void equalEntitiesStayDistinct() {
        Thing first = new Thing("same");
        Thing second = new Thing("same");
        BulkWriteFailures failures = new BulkWriteFailures(2);
        failures.recordBulkFailure(driverException(0, 0, 1), List.of(first, second), new int[] { 0, 1 });

        MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, failures::throwIfFailed);
        assertEquals(2, e.getFailures().size());
        assertEquals(List.of(0, 1), e.getFailures().stream().map(BulkWriteFailure::index).toList());
    }

    @Test
    public void noFailuresDoesNotThrow() {
        BulkWriteFailures failures = new BulkWriteFailures(3);
        failures.recordInserted(3);
        assertDoesNotThrow(failures::throwIfFailed);
    }

    @Test
    public void failureRecordRequiresExactlyOneCause() {
        assertThrows(IllegalArgumentException.class, () -> new BulkWriteFailure(new Thing("x"), 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> new BulkWriteFailure(new Thing("x"), 0,
                new BulkWriteError(11000, "dup", new BsonDocument(), 0), new IllegalStateException()));
    }

    @Test
    public void insertedCountsSum() {
        BulkWriteFailures failures = new BulkWriteFailures(6);
        failures.recordInserted(2);
        failures.recordBulkFailure(driverException(3, 3), List.of(new Thing("a"), new Thing("b"), new Thing("c"),
                new Thing("d")), new int[] { 2, 3, 4, 5 });

        MorphiaBulkWriteException e = assertThrows(MorphiaBulkWriteException.class, failures::throwIfFailed);
        assertEquals(5, e.getWriteResult().getInsertedCount());
    }

    private record Thing(String key) {
    }
}
