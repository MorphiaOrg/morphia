package dev.morphia.critter.parser

import dev.morphia.critter.Critter
import java.io.File
import org.testng.annotations.Test

class TestKspProcessor {
    @Test
    fun testExample() {
        Critter.outputDirectory = File("target/generated-test-sources/kspMain")
        Critter(File(".").canonicalFile).process()
    }
}
