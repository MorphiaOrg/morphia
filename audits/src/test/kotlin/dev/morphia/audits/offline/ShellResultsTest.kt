package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.published.*
import dev.morphia.audits.semantics.*
import dev.morphia.audits.shell.*
import org.bson.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShellResultsTest {
    private fun request(
        source: String,
        format: ProgramFormat = ProgramFormat.SHELL_RESULTS,
        action: String = "db.items.find({});",
    ) =
        ShellProgramRequest(
            dialect = ShellDialect.MONGOSH,
            blocks =
                listOf(
                    ProgramBlock("query", action, ProgramRole.ACTION),
                    ProgramBlock("results", source, ProgramRole.EXPECTED, format),
                ),
        )

    private fun run(source: String, limits: ShellProgramLimits = ShellProgramLimits()) =
        ShellPrograms.normalize(request(source), limits)

    private fun ok(source: String): ShellProgramResult =
        run(source).also {
            assertEquals(ShellLiteralState.NORMALIZED, it.state, it.problems.toString())
        }

    private fun blocked(source: String, code: String? = null) {
        val result = run(source)
        assertEquals(ShellLiteralState.UNSUPPORTED, result.state, result.toString())
        assertTrue(result.commands.isEmpty())
        assertTrue(result.expectations.isEmpty())
        assertNull(result.semanticSha256)
        assertEquals(source, result.sources.last().source)
        if (code != null) assertEquals(code, result.problems.single().code)
    }

    private fun docs(result: ShellProgramResult) = result.expectations.single().published!!

    @Test
    fun whitespaceAndCommaStreamsKeepOrderedRowsAndDuplicates() {
        for (separator in listOf("\n", "\r\n", "\u2028", "\u2029", " ", ",", ",\n", "/* gap */")) {
            val source = "{v:1}" + separator + "{v:2}" + separator + "{v:1}"
            val result = ok(source)
            assertEquals(
                listOf(1, 2, 1),
                docs(result).map { (it.fields.single().value as PublishedWholeNumber).value },
            )
            assertEquals(source, result.sources.last().source)
        }
    }

    @Test
    fun singleDocumentAndArrayFormatsHaveTheSameMeaning() {
        val stream = ok("{v:1}\n{v:2}")
        val array = ShellPrograms.normalize(request("[{v:1},{v:2}]", ProgramFormat.SHELL))
        assertEquals(array.semanticSha256, stream.semanticSha256)
        assertEquals(docs(array), docs(stream))
        assertEquals(ok("{v:1}").semanticSha256, ok("[{v:1}]").semanticSha256)
    }

    @Test
    fun delimiterCharactersInsideStringsAndCommentsAreNotBoundaries() {
        val result =
            ok(
                "// lead } ]\n {v:'http://host/{a}[x]/*y*/', quote:'\\\"}'} /* comment ] } */\n{v:'tail'} // done"
            )
        assertEquals(2, docs(result).size)
        assertEquals(PublishedText("http://host/{a}[x]/*y*/"), docs(result)[0].fields[0].value)
        assertEquals(PublishedText("\"}"), docs(result)[0].fields[1].value)
    }

    @Test
    fun nestedContainersAndConstructorArgumentsStayInsideTheirDocument() {
        val result =
            ok(
                "{v:[{x:[1,2]}], id:ObjectId('507f1f77bcf86cd799439011'), r:BSONRegExp('}[/]','im')}\n{v:3}"
            )
        assertEquals(2, docs(result).size)
        val expected = docs(result)[0]
        assertTrue(expected.fields[0].value is PublishedArray)
        assertEquals(
            BsonType.OBJECT_ID,
            (expected.fields[1].value as PublishedExact).value.bsonType,
        )
        assertEquals(
            BsonRegularExpression("}[/]", "im"),
            (expected.fields[2].value as PublishedExact).value.value(),
        )
    }

    @Test
    fun typesAreNeverGuessedThroughTheLegacyBsonReader() {
        val result = ok("{v:2}\n{v:NumberInt(2)}\n{v:Double(2)}\n{v:NumberLong('2')}")
        val values = docs(result).map { it.fields.single().value }
        assertEquals(PublishedWholeNumber(2), values[0])
        assertEquals(
            listOf(BsonType.INT32, BsonType.DOUBLE, BsonType.INT64),
            values.drop(1).map { (it as PublishedExact).value.bsonType },
        )
        assertEquals(
            BsonComparisonState.DIFFERENT,
            PublishedComparison.compareResults(
                    docs(result).take(2),
                    listOf(BsonDocument("v", BsonDouble(2.0)), BsonDocument("v", BsonDouble(2.0))),
                )
                .state,
        )
    }

    @Test
    fun emptyArrayIsKnownEmptyAndBlankTextIsNot() {
        assertTrue(docs(ok("/* known empty */ [ ] // end")).isEmpty())
        for (source in listOf("", " ", "// no value", "/* no value */")) blocked(
            source,
            "DOCUMENT_STREAM_SYNTAX",
        )
        assertEquals(1, docs(ok("{}")).size)
    }

    @Test
    fun malformedLaterRootsNeverLeakEarlierResults() {
        for (source in
            listOf(
                "{v:1}\n{v:",
                "{v:1}\n{v:'unterminated}",
                "{v:1}\n{v:[1,2}",
                "{v:1}\n/* unfinished",
                "{v:1}\n{v:1,v:2}",
                "{v:1}\n{__proto__:1}",
            )) blocked(source)
    }

    @Test
    fun streamsRequireExplicitSeparatorsAndCannotBeMixedWithArrays() {
        for (source in
            listOf(
                "{}{}",
                "{},",
                ",{}",
                "{},,{}",
                "{};{}",
                "[{}] {}",
                "{} [{}]",
                "[],{}",
                "[{}],[{}]",
                "[[{}]]",
            )) blocked(source)
        assertEquals(2, docs(ok("{},/**/{}")).size)
    }

    @Test
    fun trailingCommandsAndComputedValuesRemainUnsupported() {
        for (source in
            listOf(
                "{v:1}\ndb.items.drop()",
                "{v:1}\nprocess.exit()",
                "{v:(()=>2)()}",
                "{v:1+2}",
                "{v:function(){return 2}}",
                "{v:new Unknown(2)}",
            )) blocked(source)
    }

    @Test
    fun regexAndTemplateSyntaxFailBeforeItCanConfuseFraming() {
        for (source in listOf("{v:/[}\\]]/}", "{v:/a/}\n{v:2}", "{v:1/2}")) blocked(
            source,
            "PUBLISHED_REGEX",
        )
        for (source in listOf("{v:`} ]`}", "{v:`value ${'$'}{2}`}")) blocked(
            source,
            "UNSUPPORTED_AST",
        )
        blocked("{v:RegExp('a','i')}", "PUBLISHED_REGEX")
    }

    @Test
    fun sourceSpansReferToTheUnchangedLaterFragment() {
        val source = "/* prefix 🚀 */\r\n{v:1}\r\n {v:missing}"
        val result = run(source)
        assertEquals("UNRESOLVED_NAME", result.problems.single().code)
        val span = result.problems.single().source!!.span!!
        assertEquals("missing", source.substring(span.start, span.end))
        assertEquals(source, result.sources.last().source)
    }

    @Test
    fun unicodePrefixesAndCrLfPreserveFramingOffsets() {
        for (prefix in listOf("\uFEFF", " // 🚀 } \r\n", "/* é🚀 [ */\n", "\t\r\n")) {
            val result = ok(prefix + "{v:'é🚀'}\r\n{v:2}")
            assertEquals(2, docs(result).size)
            assertEquals(PublishedText("é🚀"), docs(result)[0].fields.single().value)
        }
    }

    @Test
    fun capturedBindingsKeepValuesTypesAndDependenciesAcrossFragments() {
        val result =
            ShellPrograms.normalize(
                request(
                    "{v:x}\n{v:NumberInt(n)}",
                    action = "let n=2; const x=n; n=3; db.items.find({});",
                )
            )
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        assertEquals(PublishedWholeNumber(2), docs(result)[0].fields.single().value)
        assertEquals(
            BsonInt32(3),
            (docs(result)[1].fields.single().value as PublishedExact).value.value(),
        )
        assertEquals(3, result.expectations.single().dependencies.size)
    }

    @Test
    fun framingAndLoweringShareWorkDepthAndStoredValueLimits() {
        val work =
            run("{v:1}\n{v:2}", ShellProgramLimits(values = ShellLiteralLimits(maximumNodes = 8)))
        assertEquals("VALUE_LIMIT", work.problems.single().code)
        val deep =
            run(
                "{v:" + "[".repeat(10) + "1" + "]".repeat(10) + "}",
                ShellProgramLimits(values = ShellLiteralLimits(maximumDepth = 5)),
            )
        assertEquals("VALUE_LIMIT", deep.problems.single().code)
        val stored =
            run(
                List(10) { "{v:'" + "x".repeat(500) + "'}" }.joinToString("\n"),
                ShellProgramLimits(maximumSnapshotBytes = 1024, maximumStoredValueBytes = 4096),
            )
        assertEquals("STORED_VALUE_LIMIT", stored.problems.single().code)
        for (result in listOf(work, deep, stored)) {
            assertTrue(result.commands.isEmpty())
            assertTrue(result.expectations.isEmpty())
        }
    }

    @Test
    fun aPublishedStreamStillRequiresTheModernReadProfile() {
        for (dialect in listOf(ShellDialect.LEGACY_MONGO, ShellDialect.UNSPECIFIED)) assertEquals(
            "PUBLISHED_PROFILE",
            ShellPrograms.normalize(request("{v:2}").copy(dialect = dialect)).problems.single().code,
        )
        assertEquals(
            "PUBLISHED_PROFILE",
            ShellPrograms.normalize(request("{v:2}", action = "db.items.insertOne({v:2});"))
                .problems
                .single()
                .code,
        )
        assertEquals(
            "PUBLISHED_PROFILE",
            ShellPrograms.normalize(
                    request("{v:2}", action = "db.items.aggregate([],{promoteValues:false});")
                )
                .problems
                .single()
                .code,
        )
    }

    @Test
    fun resultFormatCannotBecomeAnImplicitSeedOrActionGrammar() {
        for (role in listOf(ProgramRole.SETUP, ProgramRole.ACTION)) {
            val req = request("{v:2}")
            val result =
                ShellPrograms.normalize(
                    req.copy(blocks = listOf(req.blocks[0], req.blocks[1].copy(role = role)))
                )
            assertEquals(ShellLiteralState.INVALID, result.state)
            assertEquals("RESULT_FORMAT", result.problems.single().code)
        }
        val req = request("{v:2}")
        val result =
            ShellPrograms.normalize(
                req.copy(
                    blocks =
                        listOf(
                            req.blocks[0],
                            req.blocks[1].copy(namespace = LogicalNamespace(null, "items")),
                        )
                )
            )
        assertEquals("INVALID_NAMESPACE", result.problems.single().code)
    }

    @Test
    fun formatIsExplicitInStrictTransportAndIsNeverSelectedByRetry() {
        val req = request("{v:1}\n{v:2}")
        assertEquals(req, AuditJson.programRequest(AuditJson.write(req)))
        assertEquals(
            ProgramFormat.SHELL_RESULTS,
            ok(req.blocks.last().source).sources.last().format,
        )
        val ordinary =
            ShellPrograms.normalize(
                req.copy(
                    blocks = listOf(req.blocks[0], req.blocks[1].copy(format = ProgramFormat.SHELL))
                )
            )
        assertEquals(ShellLiteralState.UNSUPPORTED, ordinary.state)
        assertEquals("SYNTAX", ordinary.problems.single().code)
    }

    @Test
    fun astralIdentifiersAndCommentOffsetsKeepValuesWithoutInventedAstHints() {
        val value = ok("{𝒳:'valid'}\n{v:2}")
        assertEquals("𝒳", docs(value)[0].fields.single().name)
        val source = "{v:1, /* 🚀 */ bad:missing}"
        val result = run(source)
        assertEquals("UNRESOLVED_NAME", result.problems.single().code)
        assertNull(result.problems.single().source!!.span)
        assertEquals(source, result.sources.last().source)
        val literal = ShellLiterals.normalize(source, ShellDialect.MONGOSH)
        assertEquals("UNRESOLVED_NAME", literal.problems.single().code)
        val literalSpan = requireNotNull(literal.problems.single().span)
        assertEquals("missing", source.substring(literalSpan.start, literalSpan.end))
    }

    @Test
    fun pinnedAbsoluteTemperatureExampleNeedsNoSourceOrExpectedDataRepairs() {
        val source =
            requireNotNull(javaClass.getResource("/programs/absolute-temperature-changes.json"))
                .readText()
        val req = AuditJson.programRequest(source)
        val result = ShellPrograms.normalize(req)
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        assertEquals(req.blocks.map { it.source }, result.sources.map { it.source })
        assertEquals(
            listOf("temperatureChange", "temperatureChange"),
            result.commands.map { it.namespace.collection },
        )
        assertEquals(4, (result.commands[0].operation as InsertOperation).documents.size)
        val expected =
            listOf(30, 0, 20, 10).mapIndexed { index, delta ->
                BsonDocument("_id", BsonInt32(index + 1)).append("delta", BsonInt32(delta))
            }
        assertEquals(
            BsonComparisonState.EQUAL,
            PublishedComparison.compareResults(docs(result), expected).state,
        )
        val changed = expected.map { it.clone() }
        changed[0]["delta"] = BsonInt32(31)
        assertEquals(
            BsonComparisonState.DIFFERENT,
            PublishedComparison.compareResults(docs(result), changed).state,
        )
        assertEquals(
            BsonComparisonState.DIFFERENT,
            PublishedComparison.compareResults(docs(result), expected.reversed()).state,
        )
    }

    @Test
    fun formattingCharactersCannotMasqueradeAsDocumentSeparators() {
        for (gap in listOf("\u200d", "\u200b", "\u2060", "\udb40\udc01")) {
            blocked("{}" + gap + "{}")
        }
        for (gap in listOf("\u00a0", "\u202f", "\ufeff")) {
            assertEquals(2, docs(ok("{}" + gap + "{}")).size)
        }
    }

    @Test
    fun htmlLikeCommentsAndHashbangsCannotSilentlyHideRowsOrFields() {
        for (source in
            listOf(
                "{v:1} <!-- {v:2}",
                "{v:1}\n--> {v:2}",
                "#! {v:2}\n{v:1}",
                "{v:1, <!-- w:2\n}",
                "{v:1,\n--> w:2\n}",
            )) {
            blocked(source, "DOCUMENT_STREAM_SYNTAX")
        }
        for (source in
            listOf(
                "/* safe */{v:1}// safe\n{v:2}",
                "/** safe */{v:1}/* safe */{v:2}",
            )) assertEquals(2, docs(ok(source)).size)
    }

    @Test
    fun triviaMustBeEntirelyOwnedWhitespaceOrExplicitComments() {
        for (gap in
            listOf(
                " \u200b",
                "\n\u2060",
                "\u200d /* ok */",
                "/* ok */ \u200b",
                " \u200b, ",
                ", \u2060",
                "/*ok*/\u2060/*ok*/",
            )) blocked("{}" + gap + "{}")
        for (source in listOf("\u200b{}", "{}\u2060")) blocked(source)
        // Formatting characters inside an explicitly owned comment are harmless comment text.
        for (gap in
            listOf(
                " /* 👩‍💻 \u200b */ ",
                "// 👩‍💻 \u2060\n",
                " /* first */ , /** second */ ",
            )) assertEquals(2, docs(ok("{}" + gap + "{}")).size)
    }
}
