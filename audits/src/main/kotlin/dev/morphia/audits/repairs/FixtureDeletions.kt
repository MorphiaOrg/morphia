package dev.morphia.audits.repairs

import dev.morphia.audits.bindings.LegacyBindingCatalog
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * A legacy case deleted because no documented source reproduces its fixture: its [test] (as
 * `<type>#<method>`, null for a fixture no test owned) with the [bodySha256] the binding catalog
 * recorded, the SHA-256 of every fixture file it removed, by name, and the [reason] (the gap that
 * left it without a source).
 */
data class DeletedFixture(
    val fixture: String,
    val test: String?,
    val bodySha256: String?,
    val files: Map<String, String>,
    val reason: String,
)

data class FixtureDeletionLedger(val schemaVersion: Int = 1, val deletions: List<DeletedFixture>)

object FixtureDeletions {
    const val PATH = "audits/catalog/deleted-fixtures.json"
    private const val RESOURCES = "core/src/test/resources/dev/morphia/test/"

    /** Every legacy fixture file the deletions removed, by repository path, with its digest. */
    fun retired(ledger: FixtureDeletionLedger): Map<String, String> =
        ledger.deletions
            .flatMap { deleted ->
                deleted.files.map { (name, digest) ->
                    "$RESOURCES${deleted.fixture}/$name" to digest
                }
            }
            .toMap()

    /**
     * [catalog] without the deleted tests, each exactly as recorded, and without the unbound
     * resources of deleted fixtures no test owned.
     */
    fun remove(catalog: LegacyBindingCatalog, ledger: FixtureDeletionLedger): LegacyBindingCatalog {
        val tests = ledger.deletions.filter { it.test != null }.associateBy { it.test!! }
        val problems = mutableListOf<String>()
        tests.forEach { (test, deleted) ->
            val method = catalog.methods.singleOrNull { "${it.type}#${it.method}" == test }
            when {
                method == null -> problems += "$test is not bound"
                method.resource != deleted.fixture ->
                    problems += "$test owns ${method.resource}, not ${deleted.fixture}"
                method.bodySha256 != deleted.bodySha256 ->
                    problems += "$test changed before it was deleted"
            }
        }
        val unowned = ledger.deletions.filter { it.test == null }.map { it.fixture }.toSet()
        require(problems.isEmpty()) { problems.joinToString("\n") }
        return catalog.copy(
            methods = catalog.methods.filter { "${it.type}#${it.method}" !in tests },
            unboundResources = catalog.unboundResources.filter { it.resource !in unowned },
        )
    }

    /** Checks the ledger against the tree: every deletion is gone, recorded once, with a reason. */
    fun verify(repository: Path, ledger: FixtureDeletionLedger) {
        require(ledger.schemaVersion == 1) { "Unsupported deletion ledger ${ledger.schemaVersion}" }
        val root = repository.toRealPath()
        val problems = mutableListOf<String>()
        ledger.deletions
            .groupingBy { it.fixture }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { problems += "$it: deleted more than once" }
        ledger.deletions.forEach {
            if (root.resolve("$RESOURCES${it.fixture}").exists())
                problems += "${it.fixture}: the deleted fixture is still present"
            if (it.reason.isBlank()) problems += "${it.fixture}: a deletion needs its reason"
            if (it.files.isEmpty()) problems += "${it.fixture}: a deletion records its files"
            if ((it.test == null) != (it.bodySha256 == null))
                problems += "${it.fixture}: a deleted test records its body digest"
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }
}
