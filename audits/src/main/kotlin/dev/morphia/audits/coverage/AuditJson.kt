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
import dev.morphia.audits.api.ApiInventory
import dev.morphia.audits.naming.ExampleNameRegistry
import dev.morphia.audits.naming.NameProposal
import dev.morphia.audits.naming.NamingRequest
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

    fun hash(value: Any): String =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(write(value).toByteArray(Charsets.UTF_8))
            )

    fun catalog(json: String): CapabilityCatalog = read(json, CapabilityCatalog::class.java)

    fun plan(json: String): CoveragePlan = read(json, CoveragePlan::class.java)

    fun api(json: String): ApiInventory = read(json, ApiInventory::class.java)

    fun report(json: String): CoverageReport = read(json, CoverageReport::class.java)

    fun names(json: String): ExampleNameRegistry = read(json, ExampleNameRegistry::class.java)

    fun namingRequest(json: String): NamingRequest = read(json, NamingRequest::class.java)

    fun nameProposal(json: String): NameProposal = read(json, NameProposal::class.java)

    fun sourceRequest(json: String): GitSourceRequest = read(json, GitSourceRequest::class.java)

    fun sourceManifest(json: String): GitSourceManifest = read(json, GitSourceManifest::class.java)

    fun sourceAcquisition(json: String): SourceAcquisitionReport =
        read(json, SourceAcquisitionReport::class.java)

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
        if (type == ApiInventory::class.java) {
            val parser = tree.path("parser")
            require(
                parser.isObject &&
                    listOf("engine", "languageLevel", "runtimeVersion", "runtimeVendor").all {
                        parser.has(it)
                    }
            ) {
                "API inventory requires explicit compiler provenance"
            }
        }
        return mapper.treeToValue(tree, type)
    }
}
