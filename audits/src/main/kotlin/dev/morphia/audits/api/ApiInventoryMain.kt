package dev.morphia.audits.api

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/** Explicit read-only command; Maven never invokes it as part of the lifecycle. */
fun main(args: Array<String>) {
    require(args.size >= 3) {
        "Usage: ApiInventoryMainKt <repository-root> <revision> [--classpath-file <path>] [--classpath-entry <path>] <java-source-directory>..."
    }
    val root = Path.of(args[0])
    val sources = mutableListOf<String>()
    var classpath: List<Path> = emptyList()
    var suppliedClasspath = false
    var index = 2
    while (index < args.size) {
        when (val argument = args[index++]) {
            "--classpath-file" -> {
                require(!suppliedClasspath && index < args.size) {
                    "Supply one --classpath-file with its path"
                }
                suppliedClasspath = true
                val text = Files.readString(root.resolve(args[index++])).trimEnd('\r', '\n')
                classpath +=
                    if (text.isEmpty()) emptyList()
                    else
                        text.split(File.pathSeparatorChar).map { entry ->
                            require(entry.isNotEmpty()) {
                                "Empty classpath entries are not allowed"
                            }
                            root.resolve(entry)
                        }
            }
            "--classpath-entry" -> {
                require(index < args.size) { "Supply a path after --classpath-entry" }
                classpath += listOf(root.resolve(args[index++]))
            }
            else -> {
                require(!argument.startsWith("--")) { "Unknown option: $argument" }
                sources += argument
            }
        }
    }
    System.out.write(
        MorphiaApiInventory.capture(root, args[1], sources, classpath)
            .toJson()
            .toByteArray(Charsets.UTF_8)
    )
    System.out.flush()
}
