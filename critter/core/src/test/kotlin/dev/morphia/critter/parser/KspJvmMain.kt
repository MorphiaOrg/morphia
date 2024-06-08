package dev.morphia.critter.parser

import dev.morphia.critter.Critter
import java.io.File

fun main() {
    Critter(File(".", "critter/core").canonicalFile).process()
}
