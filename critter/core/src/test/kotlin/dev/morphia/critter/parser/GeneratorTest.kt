package dev.morphia.critter.parser

import dev.morphia.critter.Critter.Companion.critterClassLoader
import dev.morphia.critter.parser.Generators.mapper
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator as generator
import dev.morphia.critter.parser.java.CritterParser.asmify
import dev.morphia.critter.sources.Example
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel
import io.github.classgraph.ClassGraph
import java.lang.reflect.Modifier
import org.objectweb.asm.Type

object GeneratorTest {
    var entityModel: CritterEntityModel

    init {
        val classGraph = ClassGraph().addClassLoader(critterClassLoader).enableAllInfo()
        classGraph.acceptPackages("dev.morphia.critter.sources")

        classGraph.scan().use { scanResult ->
            for (classInfo in scanResult.allClasses) {
                try {
                    val name = classInfo.name
                    critterClassLoader.dump(name)
                } catch (_: Throwable) {}
            }
        }
        val generator = generator.generate(Example::class.java)

        entityModel =
            critterClassLoader
                .loadClass(generator.generatedType)
                .constructors[0]
                .newInstance(mapper) as CritterEntityModel
    }

    fun methodNames(clazz: Class<*>): Array<Array<Any>> {
        return methods(clazz)
            .map { arrayOf(it.name, it) }
            .sortedBy { it[0].toString() }
            .toTypedArray()
    }

    fun methods(clazz: Class<*>) =
        clazz.methods
            .filterNot { method -> Modifier.isFinal(method.modifiers) }
            .filter { method -> method.parameterCount == 0 }
            .filter { method -> method.declaringClass == clazz }

    fun process(resourceName: String, entity: Type, generated: Type): String {
        var asm = asmify(critterClassLoader.getResourceAsStream(resourceName).readAllBytes())

        val lines = asm.lines()
        val imports = lines.filter { it.startsWith("import ") }
        val pkg =
            Regex("^(package )(?<pkg>.*);\$").find(lines.first())!!.groups[2]!!.value + ".__morphia"
        val body =
            asm.substringAfter("{")
                .substringBeforeLast("}")
                .substringAfter("{")
                .substringBeforeLast("}")
        var header = body.substringBefore("{")
        val methods = extractMethodDefinitions(body).map { bind(it) }.toMap(LinkedHashMap())
        return ""
        //        return "package ;"}\n" +
        //            lines.drop(1)
        //                .map { it.replace("dev/morphia/critter/sources/ExampleEntityModel",
        // "generatedType.internalName") }
        //                .joinToString("\n")
        ////            .replace()
    }

    private fun bind(methodBody: String): Pair<String, String> {
        fun extractMethodName(line: String): String {
            return line.split("\"")[1]
        }

        val lines = methodBody.lines().drop(1).dropLast(1)

        return extractMethodName(lines.first()) to lines.joinToString("\n")
    }

    private fun extractMethodDefinitions(body: String): List<String> {
        val methodBody = body.substring(body.indexOf('{')).lines().toMutableList()
        var methods = listOf<String>()
        while (methodBody.first().startsWith("{")) {
            methods += methodBody.removeWhile { it.trim() != "}" }
        }

        return methods
    }
}
