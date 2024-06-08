package dev.morphia.critter.parser

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind.CLASS
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import dev.morphia.annotations.Entity
import dev.morphia.annotations.ExternalEntity

@OptIn(KspExperimental::class)
class CritterProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classes =
            resolver
                .getNewFiles()
                .flatMap {
                    it.declarations.filterIsInstance<KSClassDeclaration>().filter { klass ->
                        klass.classKind == CLASS || klass.classKind == INTERFACE
                    }
                }
                .filter {
                    it.isAnnotationPresent(Entity::class) ||
                        it.isAnnotationPresent(ExternalEntity::class)
                }
                .toList()

        //        println("**************** files[1].declarations.toList() =
        // ${klass.declarations.toList()}")
        println("**************** classes = ${classes}")

        return listOf()
    }
}
