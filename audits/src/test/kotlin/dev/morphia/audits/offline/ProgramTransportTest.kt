package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.published.*
import dev.morphia.audits.shell.*
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ProgramTransportTest {
    @TempDir lateinit var temporary: Path

    @Test
    fun declaredMultiCollectionFixtureRetainsSetupAndExpectedOwner() {
        val json = requireNotNull(javaClass.getResource("/programs/join-inventory.json")).readText()
        val result = ShellPrograms.normalize(AuditJson.programRequest(json))
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        assertEquals(
            listOf("orders", "inventory", "orders", "orders"),
            result.commands.map { it.namespace.collection },
        )
        assertEquals(
            listOf(ProgramRole.SETUP, ProgramRole.SETUP, ProgramRole.SETUP, ProgramRole.ACTION),
            result.commands.map { it.role },
        )
        assertEquals(result.commands.last().id, result.expectations.single().command)
        assertEquals(
            2,
            (result.commands.last().operation as AggregateOperation).pipeline.value().asArray().size,
        )
        assertEquals(
            20,
            (((result.expectations
                            .single()
                            .published!!
                            .single()
                            .fields
                            .single { it.name == "matches" }
                            .value as PublishedArray)
                        .items
                        .single() as PublishedDocument)
                    .fields
                    .single()
                    .value as PublishedExact)
                .value
                .value()
                .asInt32()
                .value,
        )
    }

    @Test
    fun strictSourceRequestRoundTripsAndCliNeverMutatesItsInput() {
        val request =
            ShellProgramRequest(
                dialect = ShellDialect.MONGOSH,
                blocks =
                    listOf(
                        ProgramBlock(
                            "query",
                            "const n=1; db.items.find({v:n});",
                            ProgramRole.ACTION,
                        )
                    ),
            )
        val json = AuditJson.write(request)
        assertEquals(request, AuditJson.programRequest(json))
        val input = temporary.resolve("input.json")
        input.writeText(json)
        val original = input.readBytes()
        fun run(): Pair<Int, String> {
            val out = temporary.resolve("output.json")
            val error = temporary.resolve("error.log")
            val java = Path.of(System.getProperty("java.home"), "bin", "java")
            val cp =
                System.getProperty(
                    "surefire.test.class.path",
                    System.getProperty("java.class.path"),
                )
            val process =
                ProcessBuilder(
                        java.toString(),
                        "-cp",
                        cp,
                        "dev.morphia.audits.shell.ProgramsMainKt",
                        input.toString(),
                    )
                    .redirectOutput(out.toFile())
                    .redirectError(error.toFile())
                    .start()
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                fail<Unit>("Program CLI timed out")
            }
            assertEquals("", error.readText())
            return process.exitValue() to out.readText()
        }
        val valid = run()
        assertEquals(0, valid.first, valid.second)
        assertTrue(valid.second.contains("\"state\" : \"NORMALIZED\""), valid.second)
        assertTrue(valid.second.contains("\"base64\""), valid.second)
        assertArrayEquals(original, input.readBytes())
        input.writeText(
            AuditJson.write(
                request.copy(
                    blocks = listOf(request.blocks.single().copy(source = "db.items.drop();"))
                )
            )
        )
        assertEquals(3, run().first)
        input.writeText(
            AuditJson.write(
                request.copy(
                    blocks =
                        listOf(
                            ProgramBlock(
                                "malformed",
                                "{_id:ObjectId('zz')}",
                                ProgramRole.ACTION,
                                ProgramFormat.LEGACY_DOCUMENTS,
                                LogicalNamespace(null, "items"),
                            )
                        )
                )
            )
        )
        assertEquals(3, run().first)
        input.writeText("{bad")
        assertEquals(2, run().first)
    }

    @Test
    fun unknownAndNullPrimitiveFieldsCannotWeakenTheRequestSchema() {
        val request =
            ShellProgramRequest(
                dialect = ShellDialect.MONGOSH,
                blocks = listOf(ProgramBlock("a", "db.items.find({});", ProgramRole.ACTION)),
            )
        val json = AuditJson.write(request)
        assertThrows(Exception::class.java) {
            AuditJson.programRequest(
                json.replace("\"schemaVersion\" : 1", "\"schemaVersion\" : null")
            )
        }
        assertThrows(Exception::class.java) {
            AuditJson.programRequest(json.replaceFirst("{", "{\"unknown\":1,"))
        }
        for ((field, value) in listOf("format" to "SHELL", "resultOrder" to "ORDERED")) {
            val missing = json.replace("\"$field\" : \"$value\",", "")
            assertNotEquals(json, missing)
            assertThrows(Exception::class.java) { AuditJson.programRequest(missing) }
        }
    }
}
