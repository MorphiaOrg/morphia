package dev.morphia.audits.offline

import dev.morphia.audits.repairs.ActionShape
import dev.morphia.audits.repairs.FormEquivalence
import dev.morphia.audits.repairs.FormRule
import java.nio.file.Files
import java.nio.file.Path
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Core's `FormRules` runs the same vectors, so the Kotlin rules and their Java port agree. */
class RuleVectorsTest {
    @Test
    fun goldenVectorsMatchTheFormRules() {
        val root =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("core/src/test/resources")) }
        val vectors =
            BsonDocument.parse(
                Files.readString(
                    root.resolve(
                        "core/src/test/resources/dev/morphia/test/generated/vectors/rules.json"
                    )
                )
            )
        val json = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build()
        val covered = mutableSetOf<FormRule>()
        vectors
            .getArray("vectors")
            .map { it.asDocument() }
            .forEach {
                val rules =
                    it.getArray("rules")
                        .map { rule -> FormRule.valueOf(rule.asString().value) }
                        .toSet()
                covered += rules
                val canonical =
                    FormEquivalence.canonical(
                        decode(it.getArray("input")).asArray().values,
                        ActionShape.valueOf(it.getString("shape").value),
                        rules,
                    )
                // Stored as extended JSON text: a {$regex, $options} document would be read back
                // as a regular expression.
                assertEquals(
                    it.getString("canonical").value,
                    BsonDocument("v", BsonArray(canonical)).toJson(json),
                    it.getString("name").value,
                )
            }
        // Every rewriting rule has a vector; ALTERNATIVE_COMMANDS selects a command instead.
        assertEquals(FormRule.entries.toSet() - FormRule.ALTERNATIVE_COMMANDS, covered)
    }

    companion object {
        /**
         * Reads an input key that starts with `#` with `$` in its place, since the JSON reader
         * would turn a `{$regex, $options}` document into a regular expression.
         */
        fun decode(value: BsonValue): BsonValue =
            when (value) {
                is BsonDocument ->
                    BsonDocument().also { result ->
                        value.forEach { (key, field) ->
                            result[if (key.startsWith("#")) "$" + key.drop(1) else key] =
                                decode(field)
                        }
                    }
                is BsonArray -> BsonArray(value.values.map { decode(it) })
                else -> value
            }
    }
}
