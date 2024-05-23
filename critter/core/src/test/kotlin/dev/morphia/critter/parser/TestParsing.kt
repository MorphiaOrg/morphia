package dev.morphia.critter.parser

import dev.morphia.critter.parser.java.CritterParser
import java.io.File
import java.lang.reflect.Method
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class TestParsing {
    @Test(dataProvider = "sources")
    fun basicClass(file: File) {
        println("**************** stream = ${file}")
        val klass = CritterParser.parser(file)
        klass.newInstance()
        println("**************** CritterParser.parser(stream) = $klass")

//        assertEquals(klass.name, "BasicClass")
    }

    @DataProvider
    fun sources(testMethod: Method): Array<File> {
        val name = testMethod.name.titleCase()

        println("**************** testMethod = ${testMethod}")
        return arrayOf(load(name, "Java"))
    }

    private fun load(name: String, type: String): File {
        val resource = "/dev/morphia/critter/sources/$type$name.class"
        val url = javaClass.getResource(resource) ?: throw AssertionError("$resource not found")
        return File(url.file)
    }

    private fun String.titleCase(): String {
        return first().uppercase() + substring(1)
    }
}
