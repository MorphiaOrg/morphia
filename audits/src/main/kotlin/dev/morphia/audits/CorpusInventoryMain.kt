package dev.morphia.audits

import java.nio.file.Path

/** Emits an inventory to stdout. It neither writes repository files nor contacts GitHub. */
fun main(args: Array<String>) {
    require(args.size == 2) {
        "Usage: CorpusInventoryMainKt <repository-root> <repository-revision>"
    }
    System.out.write(
        CorpusInventory.capture(Path.of(args[0]), args[1]).toJson().toByteArray(Charsets.UTF_8)
    )
    System.out.flush()
}
