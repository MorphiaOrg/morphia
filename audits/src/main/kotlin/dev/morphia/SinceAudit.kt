package dev.morphia

import dev.morphia.model.MorphiaClass
import dev.morphia.model.MorphiaMethod
import dev.morphia.model.State
import dev.morphia.model.State.ABSENT
import dev.morphia.model.State.DEPRECATED
import dev.morphia.model.State.PRESENT
import dev.morphia.model.Version
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_BRIDGE
import org.objectweb.asm.Opcodes.ACC_DEPRECATED
import org.objectweb.asm.Opcodes.ACC_PROTECTED
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.PrintWriter
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

fun main() {
    SinceAudit().run()
}

class SinceAudit {
    companion object {
        val newer = Version.v2_1_0_SNAPSHOT
        val older = Version.v1_6_0_SNAPSHOT
    }

    val classHistory = ConcurrentHashMap<String, MorphiaClass>()
    val methodHistory = ConcurrentHashMap<String, MorphiaMethod>()
    val classes = javaClass.getResourceAsStream("/audit-classes.txt")
        .bufferedReader()
        .readLines()
        .filterNot { it.startsWith("#") }
    val reports = LinkedHashMap<String, (PrintWriter) -> Boolean>()
    fun run() {
        processApis()

        validate()

        report()
    }

    fun report() {
        if (reports.isNotEmpty()) {
            val writer = PrintWriter(FileWriter("target/violations.txt"))
            try {
                val failures = mutableListOf<String>()
                reports.forEach {
                    if (it.value(writer)) {
                        failures += it.key
                    }
                }
                if (failures.isNotEmpty()) throw IllegalStateException("Violations found: ${failures}")
            } finally {
                writer.flush()
                writer.close()
            }
        }
    }

    fun processApis() {
        Version.values().forEach { version ->
            version.download().also {
                it.entries().iterator()
                    .forEach { entry ->
                        if (entry.name.endsWith("class") && !entry.name.endsWith("module-info.class")) {
                            val classNode = ClassNode().also { node ->
                                ClassReader(it.getInputStream(entry)).accept(node, SKIP_CODE)
                            }
                            val morphiaClass = classHistory.computeIfAbsent(classNode.fqcn()) { _ ->
                                MorphiaClass(classNode.packageName(), classNode.className())
                            }
                            morphiaClass.versions[version] =
                                if (classNode.access.isDeprecated()) DEPRECATED else PRESENT

                            classNode.methods.forEach { m ->
                                if (m.access.isNotPrivate() && m.access.isNotSynthetic()) {
                                    val morphiaMethod =
                                        MorphiaMethod(morphiaClass.pkgName, morphiaClass.name, m.descriptor())
                                    val method =
                                        methodHistory.computeIfAbsent(morphiaMethod.fullyQualified()) { _ ->
                                            morphiaMethod
                                        }
                                    method.versions[version] = if (m.access.isDeprecated()) DEPRECATED else PRESENT
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun validate() {
        reportMissingNondeprecatedMethods()
        reportDeprecatedMethodsStillInNew()
        reportNewDeprecatedMethods()
        reportNewDeprecatedClasses()
        reportNewMethods()
        reportNewClasses()
    }

    private fun reportMissingNondeprecatedMethods() {
        val list = methodHistory.values
            .filter { it.versions[newer] == ABSENT && it.versions[older] == PRESENT }
            .filter { classHistory["${it.pkgName}.${it.className}"]?.versions?.get(older) == PRESENT }
            .sortedBy { it.fullyQualified() }
            .filter { !it.name.startsWith("merge(Ljava/lang/Object;") }  // issue 959
            .filter { !it.name.startsWith("save(Ljava/lang/Iterable;") }  // return type changed from Iterable<Key> to List<T>
            .filter { !it.name.startsWith("save(Ljava/lang/Object;") }  // return type changed from Key to T
            .filter { !it.fullyQualified().startsWith("dev.morphia.AbstractEntityInterceptor#") }  // moved to interface
            .filter { !it.fullyQualified().startsWith("dev.morphia.query.QueryImpl#") }  // moved to interface
            .filter { noGetters(it.fullyQualified()) }
            .filter { removedFunctionality(it.fullyQualified()) }
            .filter { internalMethod(it.fullyQualified()) }
            .filter { movedToParent(it.fullyQualified()) }

        reportMethods(
            "Methods missing in ${newer} that weren't deprecated in ${older}".format(newer, older),
            older, newer, list
        )
    }

    private fun noGetters(name: String): Boolean {
        return name !in listOf(
            "dev.morphia.query.BucketAutoOptions#getGranurality()Ldev/morphia/query/BucketAutoOptions\$Granularity;"
        ) && !name.startsWith("dev.morphia.FindAndModifyOptions#get")
            && !name.startsWith("dev.morphia.FindAndModifyOptions#is")
            && !name.startsWith("dev.morphia.DeleteOptions#getCollation()Lcom/mongodb/client/model/Collation;")
            && !name.startsWith("dev.morphia.mapping.MapperOptions#get")
            && !name.startsWith("dev.morphia.mapping.MapperOptions#is")
    }

    private fun removedFunctionality(name: String): Boolean {
        return name !in listOf(
            "dev.morphia.mapping.MapperOptions\$Builder#datastoreProvider(Ldev/morphia/mapping/lazy/DatastoreProvider;)Ldev/morphia/mapping/MapperOptions\$Builder;",
            "dev.morphia.mapping.MapperOptions\$Builder#defaultMapper(Ldev/morphia/mapping/CustomMapper;)Ldev/morphia/mapping/MapperOptions\$Builder;",
            "dev.morphia.mapping.MapperOptions\$Builder#embeddedMapper(Ldev/morphia/mapping/CustomMapper;)Ldev/morphia/mapping/MapperOptions\$Builder;",
            "dev.morphia.mapping.MapperOptions\$Builder#enableCaching(Z)Ldev/morphia/mapping/MapperOptions\$Builder;",
            "dev.morphia.mapping.MapperOptions\$Builder#objectFactory(Ldev/morphia/ObjectFactory;)Ldev/morphia/mapping/MapperOptions\$Builder;",
            "dev.morphia.mapping.MapperOptions\$Builder#referenceMapper(Ldev/morphia/mapping/CustomMapper;)Ldev/morphia/mapping/MapperOptions\$Builder;",
            "dev.morphia.mapping.MapperOptions\$Builder#valueMapper(Ldev/morphia/mapping/CustomMapper;)Ldev/morphia/mapping/MapperOptions\$Builder;",
            "dev.morphia.mapping.Mapper$1#putEntity(Ldev/morphia/Key;Ljava/lang/Object;)V",
            "dev.morphia.mapping.Mapper$1#putProxy(Ldev/morphia/Key;Ljava/lang/Object;)V",
            "dev.morphia.query.UpdateOpsImpl#isIsolated()Z",
            "dev.morphia.query.UpdateOpsImpl#isolated()Ldev/morphia/query/UpdateOperations;",
            "dev.morphia.query.AbstractQueryFactory#createQuery(Ldev/morphia/Datastore;)Ldev/morphia/query/Query;",
            "dev.morphia.query.AbstractQueryFactory#createQuery(Ldev/morphia/Datastore;Lcom/mongodb/DBCollection;Ljava/lang/Class;)Ldev/morphia/query/Query;",
            "dev.morphia.query.DefaultQueryFactory#createQuery(Ldev/morphia/Datastore;Lcom/mongodb/DBCollection;Ljava/lang/Class;Lorg/bson/Document;)Ldev/morphia/query/Query;",
            "dev.morphia.query.QueryFactory#createQuery(Ldev/morphia/Datastore;Lcom/mongodb/DBCollection;Ljava/lang/Class;)Ldev/morphia/query/Query;",
            "dev.morphia.query.QueryFactory#createQuery(Ldev/morphia/Datastore;Lcom/mongodb/DBCollection;Ljava/lang/Class;Lorg/bson/Document;)Ldev/morphia/query/Query;"
        ) && !name.startsWith("dev.morphia.converters")
            && !name.contains("Converter")
            && !name.contains("EntityCache")
            && !name.startsWith("relocated")
            && !name.startsWith("dev.morphia.Morphia")
            && !name.startsWith("dev.morphia.mapping.DateStorage")
            && !name.startsWith("dev.morphia.mapping.DefaultCreator")
            && !name.startsWith("dev.morphia.mapping.MapperOptions#setCachingEnabled(Z)Ldev/morphia/mapping/MapperOptions;")
            && !name.startsWith("dev.morphia.mapping.MapperOptions#setDisableEmbeddedIndexes(Z)V")
            && !name.startsWith("dev.morphia.mapping.ReferenceMapper\$")
            && !name.startsWith("dev.morphia.mapping.EmbeddedMapper\$")
            && !name.startsWith("dev.morphia.geo.GeoJson")
            && !name.startsWith("dev.morphia.mapping.Serializer")
            && !name.startsWith("dev.morphia.query.Shape#")
            && !name.startsWith("dev.morphia.query.Shape$")
            && !name.startsWith("dev.morphia.query.UpdateResults#")
    }

    private fun internalMethod(name: String): Boolean {
        return name !in listOf(
            "dev.morphia.DeleteOptions#copy()Ldev/morphia/DeleteOptions;",
            "dev.morphia.InsertOptions#copy()Ldev/morphia/InsertOptions;",
            "dev.morphia.UpdateOptions#copy()Ldev/morphia/UpdateOptions;",
            "dev.morphia.Morphia#fromDBObject(Ldev/morphia/Datastore;Ljava/lang/Class;Lorg/bson/Document;)Ljava/lang/Object;",
            "dev.morphia.Key#<init>(Ljava/lang/Class;Ljava/lang/String;[B)V",
            "dev.morphia.aggregation.Accumulator#toDBObject()Lorg/bson/Document;",
            "dev.morphia.aggregation.AggregationElement#toDBObject()Lorg/bson/Document;",
            "dev.morphia.aggregation.AggregationElement#toDBObject()Lorg/bson/Document;",
            "dev.morphia.aggregation.AggregationPipelineImpl#<init>(Ldev/morphia/DatastoreImpl;Lcom/mongodb/DBCollection;Ljava/lang/Class;)V",
            "dev.morphia.query.FieldCriteria#getQuery()Ldev/morphia/query/QueryImpl;",
            "dev.morphia.query.FieldCriteria#toDBObject()Lorg/bson/Document;",
            "dev.morphia.query.Geo2dCriteria#toDBObject()Lorg/bson/Document;",
            "dev.morphia.query.Geo2dSphereCriteria#toDBObject()Lorg/bson/Document;",
            "dev.morphia.query.BucketAutoOptions#toDBObject()Lorg/bson/Document;",
            "dev.morphia.query.Criteria#toDBObject()Lorg/bson/Document;",
            "dev.morphia.query.UpdateOperator#fromString(Ljava/lang/String;)Ldev/morphia/query/UpdateOperator;",
            "dev.morphia.query.UpdateOpsImpl#<init>(Ljava/lang/Class;Ldev/morphia/mapping/Mapper;)V",
            "dev.morphia.query.UpdateOpsImpl#add(Ldev/morphia/query/UpdateOperator;Ljava/lang/String;Ljava/lang/Object;Z)V",
            "dev.morphia.query.UpdateOpsImpl#toDBObjList(Ldev/morphia/mapping/MappedField;Ljava/util/List;)Ljava/util/List;"
        ) && !name.startsWith("dev.morphia.logging")
            && !name.startsWith("dev.morphia.mapping.lazy")
            && !name.startsWith("dev.morphia.query.validation")
            && !name.startsWith("dev.morphia.IndexBuilder")
            && !name.startsWith("dev.morphia.IndexedBuilder")
            && !name.startsWith("dev.morphia.MapreduceResults")
            && !name.startsWith("dev.morphia.mapping.EphemeralMappedField")
            && !name.startsWith("dev.morphia.IndexOptionsBuilder")
            && !name.startsWith("dev.morphia.utils")
            && !name.startsWith("dev.morphia.mapping.experimental.CollectionReference#")
            && !name.startsWith("dev.morphia.mapping.experimental.MapReference#")
            && !name.startsWith("dev.morphia.mapping.experimental.SingleReference#")
            && !name.startsWith("dev.morphia.mapping.validation.classrules")
            && !name.startsWith("dev.morphia.mapping.validation.fieldrules")
            && !name.startsWith("dev.morphia.query.AbstractCriteria#")
            && !name.startsWith("dev.morphia.query.CriteriaContainerImpl#")
            && !name.startsWith("dev.morphia.query.FieldEndImpl#")
            && !name.startsWith("dev.morphia.mapping.validation.MappingValidator")
            && !name.startsWith("dev.morphia.query.UpdateOperator#val")
            && !name.contains(".internal")
    }

    private fun movedToParent(name: String): Boolean {
        return name !in listOf(
            "dev.morphia.DeleteOptions#getWriteConcern()Lcom/mongodb/WriteConcern;",
            "dev.morphia.UpdateOptions#getWriteConcern()Lcom/mongodb/WriteConcern;",
            "dev.morphia.query.CountOptions#getCollation()Lcom/mongodb/client/model/Collation;",
            "dev.morphia.query.CountOptions#getHint()Ljava/lang/String;",
            "dev.morphia.query.CountOptions#getLimit()I",
            "dev.morphia.query.CountOptions#getReadConcern()Lcom/mongodb/ReadConcern;",
            "dev.morphia.query.CountOptions#getReadPreference()Lcom/mongodb/ReadPreference;",
            "dev.morphia.query.CountOptions#getSkip()I",
            "dev.morphia.FindAndModifyOptions#bypassDocumentValidation(Ljava/lang/Boolean;)Ldev/morphia/FindAndModifyOptions;",
            "dev.morphia.FindAndModifyOptions#collation(Lcom/mongodb/client/model/Collation;)Ldev/morphia/FindAndModifyOptions;",
            "dev.morphia.FindAndModifyOptions#maxTime(JLjava/util/concurrent/TimeUnit;)Ldev/morphia/FindAndModifyOptions;",
            "dev.morphia.FindAndModifyOptions#upsert(Z)Ldev/morphia/FindAndModifyOptions;",
            "dev.morphia.FindAndModifyOptions#writeConcern(Lcom/mongodb/WriteConcern;)Ldev/morphia/FindAndModifyOptions;",
            "dev.morphia.UpdateOptions#getBypassDocumentValidation()Ljava/lang/Boolean;",
            "dev.morphia.UpdateOptions#getCollation()Lcom/mongodb/client/model/Collation;",
            "dev.morphia.UpdateOptions#isUpsert()Z",
            "dev.morphia.AdvancedDatastore#insert(Ljava/lang/Object;)Ldev/morphia/Key;",
            "dev.morphia.AdvancedDatastore#insert(Ljava/lang/Object;Ldev/morphia/InsertOptions;)Ldev/morphia/Key;",
            "dev.morphia.AdvancedDatastore#insert(Ljava/util/List;)Ljava/lang/Iterable;",
            "dev.morphia.AdvancedDatastore#insert(Ljava/util/List;Ldev/morphia/InsertOptions;)Ljava/lang/Iterable;"
        )
    }

    private fun reportDeprecatedMethodsStillInNew() {
        val list = methodHistory.values
            .filter { it.versions[newer] != ABSENT && it.versions[older] == DEPRECATED }
            .sortedBy { it.fullyQualified() }
        reportMethods(
            "Deprecated methods in ${older} still in ${newer}", older, newer, list,
            false
        )
    }

    private fun reportNewDeprecatedMethods() {
        reportMethods(
            "New deprecated methods in ${newer}", older, newer, newMethods(newer, older, DEPRECATED, ABSENT),
            false
        )
    }

    private fun reportNewDeprecatedClasses() {
        reportClasses(
            "New deprecated classes in ${newer}", older, newer, newClasses(newer, older, DEPRECATED, ABSENT),
            false
        )
    }

    private fun reportNewClasses() {
        reportClasses(
            "New classes in ${newer}", older, newer, newClasses(newer, older, PRESENT, ABSENT),
            false
        )
    }

    private fun reportNewMethods() {
        reportMethods(
            "New methods in ${newer}", older, newer, newMethods(newer, older, PRESENT, ABSENT),
            false
        )
    }

    private fun newMethods(newer: Version, older: Version, newState: State, oldState: State): List<MorphiaMethod> {
        return methodHistory.values
            .filter { it.versions[newer] == newState && it.versions[older] == oldState }
            .sortedBy { it.fullyQualified() }
    }

    private fun newClasses(newer: Version, older: Version, newState: State, oldState: State): List<MorphiaClass> {
        return classHistory.values
            .filter { !it.name.contains("\$") }
            .filter { it.versions[newer] == newState && it.versions[older] == oldState }
            .sortedBy { it.fqcn() }
    }

    private fun reportMethods(
        title: String, older: Version, newer: Version, list: List<MorphiaMethod>,
        failureCase: Boolean = true
    ) {
        if (list.isNotEmpty()) reports[title] = { writer ->
            write(writer, "${title}: ${list.size}")
            val versions = "%-10s %-10s"
            write(writer, versions.format("older", "newer"))
            list.forEach {
                val states = versions.format(it.versions[older], it.versions[newer])
                write(writer, "$states ${it.pkgName}.${it.className}#${it.name}")
            }
            failureCase
        }
    }

    private fun reportClasses(
        title: String, older: Version, newer: Version, list: List<MorphiaClass>,
        failureCase: Boolean = true
    ) {
        if (list.isNotEmpty()) reports[title] = { writer ->
            write(writer, "${title}: ${list.size}")
            val versions = "%-10s %-10s"
            write(writer, versions.format("older", "newer"))
            list.forEach {
                val states = versions.format(it.versions[older], it.versions[newer])
                write(writer, "$states ${it.fqcn()}")
            }
            failureCase
        }
    }

    private fun write(writer: PrintWriter, line: String) {
        println(line)
        writer.println(line)
    }
}

fun URL.download(jar: File): JarFile {
    if (!jar.exists()) {
        jar.parentFile.mkdirs()
        FileOutputStream(jar).write(readBytes())
    }
    return JarFile(jar)
}

fun ClassNode.className(): String = this.name
    .substringAfterLast("/")
    .substringBeforeLast(".")
    .replace("/", ".")

fun ClassNode.packageName(): String = this.name
    .substringAfter("classes/")
    .substringBeforeLast("/")
    .replace("/", ".")

fun ClassNode.fqcn() = "${packageName()}.${className()}"
fun MethodNode.descriptor() = name + desc
fun Int.isNotPrivate(): Boolean = isPublic() || isProtected()
fun Int.isNotSynthetic(): Boolean = !matches(ACC_BRIDGE) && !matches(ACC_SYNTHETIC)
fun Int.isDeprecated() = matches(ACC_DEPRECATED)
fun Int.isProtected() = matches(ACC_PROTECTED)
fun Int.isPublic() = matches(ACC_PUBLIC)
fun Int.matches(mask: Int) = (this and mask) == mask
fun MorphiaMethod.returnType(): String = name.substringAfterLast(")")
fun Int.accDecode(): List<String> {
    val decode: MutableList<String> = ArrayList()
    val values: MutableMap<String, Int> = LinkedHashMap()
    try {
        for (f in Opcodes::class.java.declaredFields) {
            if (f.name.startsWith("ACC_")) {
                values[f.name] = f.getInt(Opcodes::class.java)
            }
        }
    } catch (e: IllegalAccessException) {
        throw RuntimeException(e.message, e)
    }
    for ((key, value) in values) {
        if (this.matches(value)) {
            decode.add(key)
        }
    }
    return decode
}

