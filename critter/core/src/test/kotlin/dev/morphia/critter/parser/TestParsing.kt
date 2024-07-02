package dev.morphia.critter.parser

import dev.morphia.critter.parser.java.CritterParser
import java.io.File
import java.lang.reflect.Method
import org.testng.annotations.DataProvider

class TestParsing {
    init {
        System.setProperty("org.openrewrite.adapt.dumpClass", "true")
    }

    //    @Test(dataProvider = "sources")
    fun basicClass(file: File) {
        CritterParser.outputGenerated = File("target/basicClass/")
        val klass = CritterParser.parser(file)
        val newInstance = klass.newInstance()
        klass.declaredMethods.forEach { method ->
            val message = method.invoke(newInstance)
            println(message)
        }
        //        assertEquals(klass.name, "BasicClass")
    }
    /*
        @Test(enabled = false, description = "dead end test.  might revive later.")
        fun testCreateClass() {
            val s = "i'm manually created"
            val sub = object : CritterEntityModel(Mapper(MorphiaConfig.load()), JavaBasicClass::class.java) {
                override fun getEntityAnnotation(): Entity {
                    val entity = entityBuilder()
                        .value(s)
                        .build()

                    return entity
                }
            }
            val subName = sub.javaClass.name.replace('.', '/') + ".class"
            val resourceAsStream = javaClass.classLoader.getResourceAsStream(subName)

            resourceAsStream?.use { input ->
                val cl = object : ClassLoader("critter", getSystemClassLoader()) {
                    override fun findClass(name: String?): Class<*> {
                        val buffer = input.readBytes()
                        return defineClass(name, buffer, 0, buffer.size)
                    }
                }
                val loadClass = cl.loadClass(sub.javaClass.name)
                loadClass
            }
    }
    */

    @DataProvider
    fun sources(testMethod: Method): Array<File> {
        val name = testMethod.name.titleCase()

        return arrayOf(load(name, "Java") /*, load(name, "Kotlin")*/)
    }

    private fun load(name: String, type: String): File {
        val resource = "/dev/morphia/critter/sources/$type$name.class"
        val url = javaClass.getResource(resource) ?: throw AssertionError("$resource not found")
        return File(url.file)
    }
}

fun String.titleCase(): String {
    return first().uppercase() + substring(1)
}
