package dev.morphia.audits.coverage

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.type.LogicalType
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.morphia.audits.documents.DocumentReport
import dev.morphia.audits.naming.ExampleNameRegistry
import dev.morphia.audits.naming.NameProposal
import dev.morphia.audits.naming.NamingRequest
import dev.morphia.audits.scenarios.ScenarioExtractionPlan
import dev.morphia.audits.shell.ShellProgramRequest
import dev.morphia.audits.sources.GitSourceManifest
import dev.morphia.audits.sources.GitSourceRequest
import dev.morphia.audits.sources.SourceAcquisitionReport
import java.security.MessageDigest
import java.util.HexFormat

/** Versioned, strict DTO transport. No polymorphic/default typing or ambient file resolution. */
object AuditJson {
    private val mapper =
        JsonMapper.builder()
            .addModule(KotlinModule.Builder().enable(KotlinFeature.StrictNullChecks).build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build()
            .apply {
                listOf(
                        CoercionInputShape.Integer,
                        CoercionInputShape.Float,
                        CoercionInputShape.Boolean,
                    )
                    .forEach {
                        coercionConfigFor(LogicalType.Textual).setCoercion(it, CoercionAction.Fail)
                    }
            }

    fun write(value: Any): String =
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n"

    /** Count the actual UTF-8 JSON stream without allocating an amplified artifact. */
    fun requireMaximumSize(value: Any, maximumBytes: Int): Int {
        require(maximumBytes > 0)
        val output =
            object : java.io.OutputStream() {
                var count = 0L

                fun reserve(length: Int) {
                    count += length
                    if (count > maximumBytes) throw ArtifactSizeLimit(maximumBytes)
                }

                override fun write(value: Int) = reserve(1)

                override fun write(bytes: ByteArray, offset: Int, length: Int) = reserve(length)
            }
        try {
            mapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(java.io.OutputStreamWriter(output, Charsets.UTF_8), value)
            output.write('\n'.code)
        } catch (failure: com.fasterxml.jackson.databind.JsonMappingException) {
            var cause: Throwable? = failure
            repeat(16) {
                if (cause is ArtifactSizeLimit) throw cause as ArtifactSizeLimit
                cause = cause?.cause
            }
            throw failure
        }
        return output.count.toInt()
    }

    internal class ArtifactSizeLimit(maximum: Int) :
        IllegalArgumentException("Serialized artifact exceeds $maximum bytes")

    fun hash(value: Any): String =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(write(value).toByteArray(Charsets.UTF_8))
            )

    fun scenarioPlan(json: String): ScenarioExtractionPlan =
        read(json, ScenarioExtractionPlan::class.java)

    fun programRequest(json: String): ShellProgramRequest =
        read(json, ShellProgramRequest::class.java)

    fun catalog(json: String): CapabilityCatalog = read(json, CapabilityCatalog::class.java)

    fun plan(json: String): CoveragePlan = read(json, CoveragePlan::class.java)

    fun report(json: String): CoverageReport = read(json, CoverageReport::class.java)

    fun names(json: String): ExampleNameRegistry = read(json, ExampleNameRegistry::class.java)

    fun namingRequest(json: String): NamingRequest = read(json, NamingRequest::class.java)

    fun nameProposal(json: String): NameProposal = read(json, NameProposal::class.java)

    fun sourceRequest(json: String): GitSourceRequest = read(json, GitSourceRequest::class.java)

    fun sourceManifest(json: String): GitSourceManifest = read(json, GitSourceManifest::class.java)

    fun sourceAcquisition(json: String): SourceAcquisitionReport =
        read(json, SourceAcquisitionReport::class.java)

    fun documents(json: String): DocumentReport = read(json, DocumentReport::class.java)

    /** Canonical JSON attribute strings stay typed at the adapter boundary. */
    fun textValue(json: String): String = requireNotNull(mapper.readValue(json, String::class.java))

    /** The values of a JSON object's members, null where a value isn't text. */
    fun textValues(json: String): List<String?> =
        mapper.readTree(json).let { tree ->
            require(tree.isObject) { "Expected a JSON object" }
            tree.properties().map { (_, value) -> value.textValue() }
        }

    private fun <T> read(json: String, type: Class<T>): T {
        val tree = requireNotNull(mapper.readTree(json)) { "Empty JSON input" }
        require(
            tree.isObject &&
                tree.path("schemaVersion").isIntegralNumber &&
                tree.path("schemaVersion").canConvertToInt() &&
                tree.path("schemaVersion").intValue() == 1
        ) {
            "Expected explicit schemaVersion 1 for ${type.simpleName}"
        }
        if (type == ShellProgramRequest::class.java) {
            require(
                tree.has("resultOrder") &&
                    tree.path("blocks").isArray &&
                    tree.path("blocks").all { it.isObject && it.has("format") }
            ) {
                "Program requests require explicit resultOrder and block format"
            }
        }
        return mapper.treeToValue(tree, type)
    }
}
