package dev.morphia.critter.parser

import dev.morphia.critter.Critter
import java.io.File
import org.testng.annotations.Test

class TestSourceParsing {
    @Test
    fun testExampleEntity() {
        Critter.outputDirectory = File("target/generated-test-sources/morphia")
        Critter(File(".").absoluteFile).process()
    }
}
