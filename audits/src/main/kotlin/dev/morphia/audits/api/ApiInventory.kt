package dev.morphia.audits.api

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import dev.morphia.audits.CorpusFile

data class SourceLocation(val path: String, val line: Long)

/** Declared Javadoc evidence, without inferring operator contexts or version meaning. */
data class ApiTag(val name: String, val value: String)

data class ApiParameter(
    val name: String,
    val type: String,
    val declaredType: String,
    val annotations: List<String>,
    val varargs: Boolean,
)

data class ApiRecordComponent(
    val name: String,
    val type: String,
    val declaredType: String,
    val annotations: List<String>,
)

data class ApiSourceUnit(val path: String, val packageName: String, val imports: List<String>)

enum class DeclarationKind {
    TYPE,
    METHOD,
    CONSTRUCTOR,
    FIELD,
}

/**
 * Public declarations only. A member's tags belong to that member; class tags are retained on its
 * owner declaration, rather than copied onto unrelated helper methods.
 */
data class ApiDeclaration(
    val id: String,
    val kind: DeclarationKind,
    val javaKind: String,
    val owner: String,
    val name: String,
    val visibility: String,
    val parserModifiers: List<String>,
    val modifiers: List<String>,
    val annotations: List<String>,
    val typeParameters: List<String>,
    val parameters: List<ApiParameter>,
    val returnType: String?,
    val declaredReturnType: String?,
    val thrownTypes: List<String>,
    val supertypes: List<String>,
    val permittedSubtypes: List<String>,
    val annotationDefault: String?,
    val tags: List<ApiTag>,
    val documentationErrors: List<String>,
    val recordComponents: List<ApiRecordComponent>,
    val source: SourceLocation,
)

data class SourceParser(
    val engine: String = "jdk-javac",
    val languageLevel: Int = 17,
    val runtimeVersion: String = Runtime.version().toString(),
    val runtimeVendor: String = System.getProperty("java.vendor"),
)

/** Source declarations are evidence of APIs, not serialization or behavioral coverage. */
data class ApiInventory(
    val schemaVersion: Int = 1,
    val parser: SourceParser = SourceParser(),
    val repositoryRevision: String,
    val sourceRoots: List<String>,
    val sourceFiles: List<CorpusFile>,
    val classpath: List<ClasspathInput>,
    val sourceUnits: List<ApiSourceUnit>,
    val declarations: List<ApiDeclaration>,
) {
    fun toJson(): String = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(this) + "\n"

    companion object {
        internal val operatorTags =
            setOf("aggregation.expression", "aggregation.stage", "query.filter", "update.operator")

        private val JSON =
            JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build()
    }
}
