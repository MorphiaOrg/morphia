# Bulk write failures and retries

Origin: https://github.com/MorphiaOrg/morphia/issues/3360 (milestone 3.0.0)

Status: **implemented**

## Problem

A common use of `Datastore.insert(List)` / `Datastore.save(List)` is ingesting a large batch of records into a
collection with a unique index, where some records in the batch may already be stored (re-delivered data, overlapping
imports) or may collide with each other. The typical failure is a duplicate key (`E11000`) on a user-defined unique
index.

What applications need from a batch write in that situation:

1. **Keep going past bad records.** One duplicate shouldn't prevent every valid record after it from being stored.
2. **Know exactly which entities were not stored**, as entity objects from the list they passed in, so they can log,
   discard, or retry them.
3. **Retry safely.** Retrying the not-stored entities must not double-insert anything and must not turn into a
   different kind of write.
4. **Stay fast.** Falling back to one insert per entity is several times slower on batches of thousands, so the
   solution can't be "write them one at a time."

Today none of 2–3 is reliable. On failure the caller gets the driver's `MongoBulkWriteException`, whose error indexes
don't necessarily point at the caller's entities (see Gaps), and Morphia stops partway through without saying what it
skipped. Parsing error messages and retrying the list re-sends entities that were already stored, which then fail as
duplicates themselves.

"Failure" here means any per-document write error in a partially-successful batch: unique-index violations (same
indexed values as a stored document or as an earlier document in the batch), duplicate `_id`, document-validation
failures. The design treats them all the same way.

## What already exists

`InsertManyOptions.ordered(false)` is public and passed straight to the driver. With it, the server attempts every
document, stores all the valid ones, and reports every failure by index in one `MongoBulkWriteException`. That covers
need 1 (and need 4: one round trip) without any retry loop. It isn't mentioned in Morphia's docs and has no
Morphia-level test.

## Gaps Morphia adds on top of the driver

All in `core/src/main/java/dev/morphia/MorphiaDatastore.java` (`insert(List, InsertManyOptions)`,
`save(List, InsertManyOptions)`).

1. **Indexes are relative to a per-group sub-list.** Both `insert(List)` and `save(List)` group entities (today by
   class, see gap 5) and issue one `insertMany` per group. A `BulkWriteError.getIndex()` points into that group's
   sub-list, not the caller's list. Correct when the list forms a single group; wrong otherwise. Grouping by
   collection (gap 5) makes this rarer but doesn't remove it.
2. **A failure stops all remaining work.** If one group's `insertMany` throws, later groups are never sent, and in
   `save(List)` the entities routed to one-by-one `save(entity)` (those with an id or a `@Version` property) are never
   saved. Under `ordered(false)` the caller expects everything to be attempted.
3. **Version rollback is over-broad.** `insert(List)` rolls back the version bump on *every* entity in the failing
   group's batch, including those that were stored. Their in-memory `@Version` then disagrees with the database.
   (`save(List)`'s bulk path never contains versioned entities, so this is `insert(List)` only.)
4. **Rejected entities keep a generated `_id`.** `MorphiaCodec` is a `CollectibleCodec`; the driver calls
   `generateIdIfAbsentFromDocument` on every document before sending, so a rejected or unattempted entity comes back
   with an `_id` even though nothing was stored. Passing it to `save()` later takes the "has an id" path (replace with
   upsert) instead of insert. *To be confirmed by a test before relying on it.*
5. **Batches are per Java class, not per collection.** Grouping keys on `entity.getClass()`, so subclasses stored in
   the same collection, and unrelated `@Entity` classes mapped to the same collection name, each get their own
   `insertMany`. That's extra round trips, and it makes "ordered" mean "ordered within a class," which isn't what a
   caller passing one list expects.

## Decisions

Decided:

- **Keep throwing.** A batch where any entity fails still throws. No result-object return path.
- **Option B.** Morphia throws its own exception carrying the failures in terms of the caller's entities. Chosen over
  "fix the indexes inside the driver exception" (option A) because fixing gaps 1 and 2 means aggregating results from
  several `insertMany` calls, which a single driver `MongoBulkWriteException` can't represent honestly (one
  `BulkWriteResult`, one server address), and because translating indexes back to entities is exactly the error-prone step callers get wrong.
- **No auto-resume, in any form.** An alternative considered was to keep ordered writes per batch and,
  on failure, map the index back and resume `insertMany` with the rest of the batch. Option B's index mapping would
  make that easy to build, but it's rejected, whether as the default or as an opt-in:
  - It stores the same set of documents as `ordered(false)`, at k+1 round trips for k failures instead of one.
  - It degrades badly when re-ingesting mostly-stored data, the main use case above: each resume re-sends the
    remaining tail, so n duplicates in a batch of N cost n round trips and ~n·N/2 documents sent. 10k all-duplicate
    records means 10k round trips and ~50M documents, worse than simply inserting one at a time.
  - As a default it would contradict "stays ordered" (stop at first failure). As an opt-in it adds a third mode beside
    `ordered(true)`/`ordered(false)` with subtle differences to explain.
  - Its only advantage over unordered is guaranteed processing order (e.g. first of two same-key documents in a batch
    wins). Unique indexes on sharded collections must be prefixed by the shard key, so colliding documents hit the same
    shard and are applied in order in practice. That isn't verified or documented by MongoDB, which is why the
    escape hatch below exists.
  - Callers who need strict order can resume themselves with the new exception (safe because its indexes and entity
    lists refer to the caller's list). The docs show this pattern:

    ```java
    List<Reading> remaining = readings;
    while (!remaining.isEmpty()) {
        try {
            datastore.save(remaining);
            remaining = List.of();
        } catch (MorphiaBulkWriteException e) {
            log(e.getFailures());
            remaining = e.getUnprocessedEntities();
        }
    }
    ```

- **The default stays ordered** (the driver default). A plain `save(list)` still stops at the first failure; the new
  exception reports what was skipped. Switching list saves to unordered by default was considered and rejected:
  it diverges from the driver and MongoDB docs, breaks callers relying on stop-on-error or first-wins semantics, and
  it's a silent behavior change (code still compiles but writes different data on failure) that migration tooling
  can't flag. A datastore-wide config switch was also rejected (action at a distance). Because of this,
  "keep going past bad records" (need 1) requires opting in with `ordered(false)`, and the javadoc and docs must point
  there (see Docs).

- **Batch by collection** (gap 5), not by class. See "Batching by collection".
  Grouping key is (collection, write concern).

Builder's calls (flag in the PR for review):

- The exception **extends `MongoBulkWriteException`** so existing `catch (MongoBulkWriteException e)` code keeps
  working.
- An ordered `save(List)` that hits a version conflict throws `MorphiaBulkWriteException`, not a bare
  `VersionMismatchException` (see "`save(List)` one-by-one path").
- Inside a transaction, list writes stop at the first failure even when unordered (see "Inside a transaction").

## Design

### Batching by collection

Both `insert(List)` and `save(List)`'s bulk path group entities by a key of **(resolved collection name, effective
write concern)** and issue one `insertMany` per group.

- **Resolved collection name**: `options.collection()` if set, otherwise the entity's mapped collection name. Not the
  class hierarchy: a subclass may declare its own collection, and unrelated classes may share one.
- **Effective write concern**: the `InsertManyOptions` write concern if set, otherwise the entity class's `@Entity`
  write concern (what `getCollection(type)` applies today), otherwise the database default. Two classes sharing a
  collection but declaring different write concerns stay in separate groups so neither setting is silently dropped.
  In the normal case this is still one group per collection.
- Group order is first-appearance order of the key in the caller's list.

Encoding a mixed group: the group's `insertMany` runs on `getDatabase().getCollection(name, Object.class)`, configured
with the options and write concern as today, and given a codec registry whose `Object` codec is a small
**dispatching `CollectibleCodec`**. For each document it looks up the codec for `value.getClass()` in Morphia's
registry and delegates `encode`, `generateIdIfAbsentFromDocument`, `documentHasId` and `getDocumentId`. Decoding isn't
needed (inserts don't decode). This handles subclasses and unrelated classes uniformly, and lifecycle callbacks run
exactly once because each document goes straight to its own class's codec. (The existing base-class → subtype
delegation in `EntityEncoder`/`LifecycleEncoder` would cover subclasses but not unrelated classes sharing a
collection.)

Builder's call: single-class groups can take the same path rather than special-casing them; switch to the existing
typed `getCollection(type)` only if benchmarks show the dispatch lookup costs anything measurable.

Consequence: an ordered failure now stops the rest of the whole collection's batch, which may include other classes,
rather than only that class's batch. That's the intended meaning of "ordered" for one list.

### New exception: `dev.morphia.MorphiaBulkWriteException`

```java
public class MorphiaBulkWriteException extends MongoBulkWriteException {
    /** Entities the server rejected, with the error and their index in the caller's list. */
    public List<BulkWriteFailure> getFailures();

    /** Convenience: just the rejected entities, in caller-list order. */
    public <T> List<T> getFailedEntities();

    /** Entities never sent because an ordered write stopped first. Always empty when ordered(false). */
    public <T> List<T> getUnprocessedEntities();
}

/** Exactly one of error/exception is non-null. */
public record BulkWriteFailure(Object entity, int index, @Nullable WriteError error,
                               @Nullable RuntimeException exception) {}
```

- `error` is set for server write errors (duplicate key, validation, ...). `exception` is set for failures that have no
  `WriteError`, today only `VersionMismatchException` from the one-by-one path in `save(List)` (see below).
- Java exceptions can't be generic, hence `Object` and the unchecked generic accessors.
- Failures are a list of records, **not** a `Map<entity, error>`: user entities often define `equals`/`hashCode` on the
  very fields that collided, so a map would merge distinct failures.
- `index` is the position in the list the caller passed, so `getUnprocessedEntities()` plus `getFailedEntities()` is
  exactly "what isn't in the database" (need 2), and retrying them is safe (need 3).

Parent-class fields, built from the per-group driver exceptions:

- `getWriteErrors()`: every `BulkWriteError`, **re-indexed to the caller's list**, sorted by index. This also fixes gap
  1 for code that only catches the driver type.
- `getWriteResult()`: `BulkWriteResult.acknowledged(...)` with the summed inserted count across all groups (and
  replace/upsert counts from the one-by-one path in `save(List)`), or `unacknowledged()` if any part was.
- `getWriteConcernError()`: the first one seen (log the rest).
- `getServerAddress()`: from the first failing batch.
- Error labels: union of all batches.
- Message: Sofia-localized summary ("N of M entities failed to save; see getFailures()") rather than the first driver
  message.

### Bookkeeping

While grouping, keep a parallel `List<Integer>` per group holding each entity's position in the caller's list. When a
group's `insertMany` throws `MongoBulkWriteException`, map each `BulkWriteError.getIndex()` through that group's index
list. Positions rather than an identity map, because neither `equals` (see above) nor identity is safe: the same object
can appear twice in a list (the second is a duplicate `_id` failure and must be reported at its own index). The
"had a null id before the write" bookkeeping for generated ids is keyed the same way.

### Ordered vs. unordered

| | `ordered(true)` (default) | `ordered(false)` |
|---|---|---|
| Failing group | entities before the error index stored; error index failed; rest of group unprocessed | every valid entity stored; each error is a failure |
| Later groups / one-by-one saves | not attempted → unprocessed | attempted; their failures collected |
| Thrown | after the first failing group | once, after everything has been attempted |

Groups are (collection, write concern) as described in "Batching by collection", in first-appearance order. "Ordered"
is therefore per collection, not across the whole list, and `save(List)` additionally sends all new entities (bulk)
before the ones with an id or version (one by one). Keep that order; document it.

### Version rollback (gap 3)

In `insert(List)`, on `MongoBulkWriteException` roll back only the failed and unprocessed entities. On any other
`MongoException` (network, timeout — outcome unknown) keep today's behavior of rolling back the whole group.

### Generated ids (gap 4)

Decided: clear generated ids.

Record which entities had a null id before the write (an identity set, same reason as the index bookkeeping). After a
failure, for every failed or unprocessed entity in that set, set the id property back to null so a retry inserts rather
than replace-with-upsert. Ids the caller assigned themselves are never touched. Stored entities keep their generated
ids.

This also covers the one-by-one path in `save(List)`: a versioned entity with a null id goes through `insertOne`, which
generates an id the same way.

The first test written should confirm the premise (the driver assigns ids to every document up front, including those
that end up rejected or unsent). If it turns out unsent documents never get an id, the reset is still correct, just a
no-op for them.

### `save(List)` one-by-one path

Entities with an id or a `@Version` go through `save(entity)` individually. Two kinds of per-entity failure are
recorded:

- `MongoWriteException`: recorded with `error = e.getError()`.
- `VersionMismatchException` (decided): recorded with `exception = e`. `save(entity)` already rolls
  the entity's version back before throwing, so nothing extra is needed. Version conflicts have no `BulkWriteError`, so
  they appear in `getFailures()`/`getFailedEntities()` but not in the inherited `getWriteErrors()`. The exception
  message counts them separately.

Under `ordered(false)`, record and continue. Under ordered, the first failure of either kind makes the rest
unprocessed.

Builder's call: under ordered, a version conflict is also wrapped in `MorphiaBulkWriteException` rather than thrown
bare, so callers get the unprocessed entities and one exception type for `save(List)` in both modes. The first
`VersionMismatchException` is set as the exception's cause. This changes behavior for code that catches
`VersionMismatchException` around `save(List)` (it no longer fires), so it goes in `migrating.md`. Single-entity
`save(entity)` is unchanged.

### Inside a transaction

A server write error aborts a MongoDB transaction, so nothing the batch "stored" will be committed, and further writes in
the same transaction fail. Inside a session with an active transaction (`MorphiaSession`), stop at the first failing
group/entity regardless of `ordered`: report its failures, mark everything not yet sent as unprocessed, and throw.

Resetting for retry (refined during review):

- After a **server write error**, or any `MongoException` escaping the write (a write conflict labeled
  `TransientTransactionError`, say), the transaction is aborted, so reset **every** entity in the write: roll back
  version bumps and clear generated ids, not just the listed ones. Otherwise the driver's `withTransaction()` retry
  would reuse ids and versions from the aborted attempt.
- A **version conflict** is detected client-side and doesn't abort the transaction, so only the conflicting entity is
  reset; entities saved before it keep their ids and versions and can still commit.
- `save(List)` snapshots each individually saved entity's version before its save (shared by identity, so an instance
  listed twice restores to its version before the first save), so the reset can restore versions on that path too.

The exception's javadoc and the docs say that "stored" entities are only stored if the transaction commits.

### Non-bulk errors

Anything that isn't a per-entity write error (connection failures, timeouts, `MongoCommandException`, a
write-concern-only `MongoBulkWriteException`, client-side encoding or lifecycle failures) propagates unchanged and
immediately: we can't say what the failing batch stored. Refined during review: before rethrowing, Morphia still

- rolls back the failing batch's version bumps (outcome unknown, as before);
- records every later batch or individual entity as unprocessed (they were never sent);
- resets the entities already known not to be stored, and attaches a `MorphiaBulkWriteException` describing them as a
  suppressed exception, so callers can recover what is known.

## Open questions

1. ~~**`VersionMismatchException` inside `save(List)` under `ordered(false)`.**~~ Decided: record it and keep going
   (see "`save(List)` one-by-one path").
2. ~~**Gap 4 id reset.**~~ Decided: clear generated ids on failed/unprocessed entities (see "Generated ids").
3. ~~**Exception name/package.**~~ Decided: `dev.morphia.MorphiaBulkWriteException` and `dev.morphia.BulkWriteFailure`.

## Out of scope

- Changing `insert(List)`'s `void` return or `save(List)`'s return value.
- Pre-filtering duplicates client-side (querying for existing keys before inserting) — that's an application concern.
- 2.x backport: this changes exception types, so 3.0 only.

## Tests (both mappers, critter first)

In `core/src/test/java/dev/morphia/test/` alongside existing insert/save tests:

- Batching: a subclass and its parent in one collection, and two unrelated classes mapped to the same collection, go
  out in one `insertMany` (assert via a command listener); `@PrePersist`/`@PostPersist` fire exactly once per entity;
  a subclass with its own `@Entity` collection is batched separately; two classes sharing a collection with different
  `@Entity` write concerns are batched separately.
- Ordered failure in a mixed-class collection batch: entities of the other class after the failure are unprocessed.
- Single type, unique index, duplicate at the start/middle/end: ordered → correct stored/failed/unprocessed;
  unordered → all non-duplicates stored, failures reported with caller indexes.
- Two collections with a failure in the first: ordered → second collection unprocessed; unordered → second stored.
  Assert `getWriteErrors()` indexes point at the right entities in the caller's list.
- `save(List)` mixing new entities with id'd/versioned ones; failure in each path.
- `save(List)` with a stale `@Version` entity in the middle: unordered → the conflict is a failure with `exception`
  set, entities after it are stored, version rolled back; ordered → wrapped, rest unprocessed, cause is the
  `VersionMismatchException`.
- `@Version` entities in `insert(List)`: stored entities keep their bumped version; failed ones are rolled back.
- Generated id is cleared on failed/unprocessed entities; retrying them with `save(List)` stores them with no duplicates.
- Existing `catch (MongoBulkWriteException)` code (e.g. `TestDocumentValidation`) still passes.
- Within a transaction (`MorphiaSession`), unordered list spanning two collections: stops at the first failing group, later groups
  unprocessed, no follow-up writes attempted.
- Same object instance twice in one list: second occurrence reported as a failure at its own index.

## Docs

Since the default stays ordered, the docs carry the "keep going past duplicates" story.

**Javadoc** (`Datastore.insert(List, InsertManyOptions)`, `save(List, InsertManyOptions)`, and the no-options
overloads):

- State that writes are ordered by default and stop at the first failure, and that ordering is per collection when
  the list spans collections.
- Point to `InsertManyOptions.ordered(false)` to store every valid entity in one round trip.
- `@throws MorphiaBulkWriteException` describing failed vs. unprocessed entities.
- `InsertManyOptions.ordered(boolean)`: expand the one-liner to explain both modes and link back to the above.

**`morphia-docs`** (`docs-redesign` branch, `content/morphia/3.0/`). No page covers list insert/save error handling
today.

- New section, probably a "Saving lists of entities" part of `quicktour.md` or a dedicated saving page:
  - ordered (default) vs. `ordered(false)`, with the batch-ingest-into-a-unique-index scenario from Problem as the example;
  - the recommended batch pattern: `ordered(false)`, catch `MorphiaBulkWriteException`, log/handle `getFailures()`;
  - what `getFailedEntities()` vs. `getUnprocessedEntities()` mean, and that retrying the union is safe (no
    double-inserts; Morphia clears ids it generated on those entities, caller-assigned ids are left alone);
  - lists spanning collections: one batch per collection (and write concern), and what "ordered" means then;
  - "if you need strict order": the manual resume-from-`getUnprocessedEntities()` snippet from Decisions, with a note
    that it costs a round trip per failure and why Morphia doesn't do it automatically.
- `migrating.md`: list saves/inserts now throw `MorphiaBulkWriteException` (a `MongoBulkWriteException` subclass, so
  existing catches still work), and error indexes now refer to the caller's list. Also: a version conflict inside
  `save(List)` now arrives as a `MorphiaBulkWriteException` (with the `VersionMismatchException` as a failure and as
  the cause), not a bare `VersionMismatchException`.

Cross-link the code and docs PRs.
