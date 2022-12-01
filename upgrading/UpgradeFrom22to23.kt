///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.jetbrains.kotlin:kotlin-stdlib:1.7.21
//DEPS org.zeroturnaround:zt-exec:1.12

import java.io.File
import org.zeroturnaround.exec.ProcessExecutor
import java.util.concurrent.TimeUnit.SECONDS

var updates: List<Pair<String, String>> = listOf(
    "dev.morphia.annotations.experimental.Name" to "dev.morphia.annotations.Name",
    "dev.morphia.annotations.experimental.IdField" to "dev.morphia.annotations.IdField",
    "dev.morphia.query.experimental" to "dev.morphia.query",
    "dev.morphia.aggregation.experimental" to "dev.morphia.aggregation",
    "dev.morphia.query.experimental.updates" to "dev.morphia.query.updates",
)

fun updatePackages(file: File) {
    val original = file.readLines()
    val lines = original
        .map { line ->
            var replaced = line
            updates.forEach {
                replaced = replaced.replace(it.first, it.second)
            }
            replaced
        }
    if(lines != original) {
        println("Updating $file")
        file.writeText(lines.joinToString("\n"))
    }
}

fun updatePom() {
    ProcessExecutor()
        .commandSplit("mvn -q versions:use-latest-releases versions:update-properties -Dincludes=dev.morphia.morphia:*")
        .redirectOutput(System.out)
        .destroyOnExit()
        .start()
        .process
        .waitFor(30, SECONDS)
}

fun updateSources() {
    File(".")
        .walkTopDown()
        .filter { it.isFile }
        .filter { it.extension in listOf("java", "kt") }
        .forEach {
            updatePackages(it)
        }
}

fun main() {
    updateSources()
    updatePom()
}
