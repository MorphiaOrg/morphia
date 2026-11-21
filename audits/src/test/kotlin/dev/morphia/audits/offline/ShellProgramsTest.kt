package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.published.*
import dev.morphia.audits.shell.*
import org.bson.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShellProgramsTest {
    private fun run(
        code: String,
        dialect: ShellDialect = ShellDialect.MONGOSH,
    ): ShellProgramResult =
        ShellPrograms.normalize(
            ShellProgramRequest(
                dialect = dialect,
                blocks = listOf(ProgramBlock("action", code, ProgramRole.ACTION)),
            )
        )

    private fun ok(code: String, dialect: ShellDialect = ShellDialect.MONGOSH): ShellProgramResult {
        val result = run(code, dialect)
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        return result
    }

    private fun blocked(code: String, expected: String? = null) {
        val result = run(code)
        assertEquals(ShellLiteralState.UNSUPPORTED, result.state, result.toString())
        assertTrue(result.commands.isEmpty())
        assertTrue(result.expectations.isEmpty())
        assertNull(result.semanticSha256)
        if (expected != null)
            assertEquals(expected, result.problems.single().code, result.toString())
    }

    @Test
    fun displayedWholeNumbersNeverInheritInputSerializationTypes() {
        fun result(source: String, dialect: ShellDialect = ShellDialect.MONGOSH) =
            ShellPrograms.normalize(
                ShellProgramRequest(
                    dialect = dialect,
                    blocks =
                        listOf(
                            ProgramBlock("action", "db.items.aggregate([]);", ProgramRole.ACTION),
                            ProgramBlock("result", source, ProgramRole.EXPECTED),
                        ),
                )
            )
        val ambiguous = result("[{avg:2}]")
        assertEquals(ShellLiteralState.NORMALIZED, ambiguous.state)
        assertNull(ambiguous.expectations.single().documents)
        assertEquals(
            PublishedWholeNumber(2),
            ambiguous.expectations.single().published!!.single().fields.single().value,
        )
        for (dialect in listOf(ShellDialect.LEGACY_MONGO, ShellDialect.UNSPECIFIED)) {
            assertEquals("PUBLISHED_PROFILE", result("[{avg:2}]", dialect).problems.single().code)
        }
        for ((source, type) in
            listOf(
                "NumberInt(2)" to BsonType.INT32,
                "Double(2)" to BsonType.DOUBLE,
                "2.5" to BsonType.DOUBLE,
            )) {
            val typed = result("[{avg:$source}]")
            assertEquals(ShellLiteralState.NORMALIZED, typed.state, typed.problems.toString())
            val constraint =
                typed.expectations.single().published!!.single().fields.single().value
                    as PublishedExact
            assertEquals(type, constraint.value.bsonType)
        }
    }

    @Test
    fun malformedLegacyValuesCannotCrashTheWholeProgram() {
        val request =
            ShellProgramRequest(
                dialect = ShellDialect.MONGOSH,
                blocks =
                    listOf(
                        ProgramBlock(
                            "setup",
                            "{_id:ObjectId('zz')}",
                            ProgramRole.SETUP,
                            ProgramFormat.LEGACY_DOCUMENTS,
                            LogicalNamespace(null, "items"),
                        ),
                        ProgramBlock("action", "db.items.find({});", ProgramRole.ACTION),
                    ),
            )
        val result = ShellPrograms.normalize(request)
        assertEquals(ShellLiteralState.UNSUPPORTED, result.state)
        assertEquals("LEGACY_DOCUMENTS", result.problems.single().code)
        assertEquals(2, result.sources.size)
        assertTrue(result.commands.isEmpty())
    }

    @Test
    fun pinnedNamespacePredicatesAndExplainReturnTypesAreEnforced() {
        for (name in listOf("a.b", "a b", "a/b", "a\\b", "a\"b")) {
            blocked(
                "db.getSiblingDB(" + AuditJson.write(name).trim() + ").items.find({});",
                "NAMESPACE_NAME",
            )
        }
        blocked("db.getCollection('a${'$'}b').find({});", "NAMESPACE_NAME")
        blocked("db['a${'$'}b'].find({});", "NAMESPACE_NAME")
        for (value in listOf("true", "false", "'queryPlanner'")) blocked(
            "db.items.aggregate([],{explain:" + value + "}).toArray();",
            "AGGREGATE_EXPLAIN",
        )
        ok("db.getCollection('a.b').find({});")
    }

    @Test
    fun unsupportedProgramsKeepEveryValidatedSourceBlock() {
        val blocks =
            listOf(
                ProgramBlock("unsupported", "db.items.drop();", ProgramRole.ACTION),
                ProgramBlock("later", "db.items.find({});", ProgramRole.ACTION),
            )
        val result =
            ShellPrograms.normalize(
                ShellProgramRequest(dialect = ShellDialect.MONGOSH, blocks = blocks)
            )
        assertEquals(ShellLiteralState.UNSUPPORTED, result.state)
        assertEquals(blocks.map { it.source }, result.sources.map { it.source })
        assertTrue(result.commands.isEmpty())
        assertTrue(result.expectations.isEmpty())
    }

    @Test
    fun preservesOrderedSetupIndexesQueryOptionsAndMultipleCollections() {
        val code =
            """
            db.items.insertMany([{_id:1,v:2},{_id:2,v:3}]);
            db.other.insertOne({_id:9,v:4});
            db.items.createIndex({v:1,_id:-1},{unique:true});
            db.items.find({v:{${'$'}gt:1}},{_id:0,v:1}).sort({v:1,_id:-1}).limit(2).toArray();
            """
                .trimIndent()
        val result = ok(code)
        assertEquals(
            listOf(
                ShellOperationKind.INSERT,
                ShellOperationKind.INSERT,
                ShellOperationKind.INDEX,
                ShellOperationKind.FIND,
            ),
            result.commands.map { it.operation.kind },
        )
        assertEquals(
            listOf("items", "other", "items", "items"),
            result.commands.map { it.namespace.collection },
        )
        val index = result.commands[2].operation as IndexOperation
        assertEquals(listOf("v", "_id"), index.keys.value().asDocument().keys.toList())
        val query = result.commands.last().operation as FindOperation
        assertEquals(2, query.modifiers.last().number)
        assertEquals(
            listOf(QueryModifierKind.SORT, QueryModifierKind.LIMIT),
            query.modifiers.map { it.kind },
        )
    }

    @Test
    fun capturedBindingsPreservePrimitiveAndExplicitBsonConstructorMeaning() {
        val result =
            ok(
                "const n=1; const rows=[{plain:n,forced:Double(n)}]; db.items.insertMany(rows); db.items.find({plain:n});"
            )
        val insert = result.commands.first().operation as InsertOperation
        val document = insert.documents.single().value().asDocument()
        assertEquals(BsonType.INT32, document["plain"]!!.bsonType)
        assertEquals(BsonType.DOUBLE, document["forced"]!!.bsonType)
        assertTrue(result.bindings[1].dependencies.contains(result.bindings[0].id))
        assertTrue(result.commands.first().dependencies.contains(result.bindings[1].id))
        val unknown =
            ok("const n=1; db.items.find({forced:NumberInt(n)});", ShellDialect.UNSPECIFIED)
        assertEquals(
            BsonType.INT32,
            (unknown.commands.single().operation as FindOperation)
                .filter
                .value()
                .asDocument()["forced"]!!
                .bsonType,
        )
    }

    @Test
    fun reassignmentCannotRetroactivelyChangeCapturedValues() {
        val result = ok("let x=1; const y=x; x=2; db.items.find({first:y,current:x});")
        val filter =
            (result.commands.single().operation as FindOperation).filter.value().asDocument()
        assertEquals(1, filter["first"]!!.asInt32().value)
        assertEquals(2, filter["current"]!!.asInt32().value)
        assertEquals(3, result.bindings.size)
        blocked("const x=1; x=2; db.items.find({x:x});", "ASSIGNMENT_BINDING")
        blocked("let x=1; let x=2; db.items.find({x:x});")
        blocked("const x=y; const y=1; db.items.find({x:x});", "UNRESOLVED_NAME")
    }

    @Test
    fun sloppyAssignmentDeclaresAMutableGlobal() {
        val result =
            ok("stage={${'$'}match:{a:1}}; stage={${'$'}match:{a:2}}; db.items.aggregate([stage]);")
        val pipeline = (result.commands.single().operation as AggregateOperation).pipeline.value()
        assertEquals(BsonDocument.parse("{v: [{${'$'}match: {a: 2}}]}").getArray("v"), pipeline)
        assertEquals(
            listOf(ProgramDeclaration.ASSIGNMENT, ProgramDeclaration.ASSIGNMENT),
            result.bindings.map { it.declaration },
        )
        // A lexical declaration after the implicit global is still a redeclaration.
        blocked("x=1; let x=2; db.items.find({x:x});", "DUPLICATE_BINDING")
        blocked("db=1; db.items.find({});", "RESERVED_BINDING")
    }

    @Test
    fun aliasesTrackDatabaseCollectionAndValueDependencies() {
        val result =
            ok(
                "const name='items'; const d=db.getSiblingDB('other'); const c=d.getCollection(name); c.find({});"
            )
        assertEquals(LogicalNamespace("other", "items"), result.commands.single().namespace)
        assertEquals(ProgramBindingKind.COLLECTION, result.bindings.last().kind)
        assertEquals(2, result.bindings.last().dependencies.size)
        assertEquals(listOf(result.bindings.last().id), result.commands.single().dependencies)
        blocked("db.stats.find({});", "DATABASE_PROPERTY")
        assertEquals(
            "stats",
            ok("db.getCollection('stats').find({});").commands.single().namespace.collection,
        )
    }

    @Test
    fun aggregationAndBothUpdateFormsKeepTheirDataAndOptions() {
        val result =
            ok(
                """
                const pipeline=[{${'$'}match:{v:1}},{${'$'}sort:{v:-1,_id:1}}];
                db.items.aggregate(pipeline,{allowDiskUse:true});
                db.items.updateMany({v:1},{${'$'}set:{v:2}},{upsert:false});
                db.items.updateOne({_id:1},[{${'$'}set:{v:3}}]);
                """
                    .trimIndent()
            )
        assertEquals(3, result.commands.size)
        assertEquals(
            BsonType.ARRAY,
            (result.commands[2].operation as UpdateOperation).update.bsonType,
        )
        val aggregate = result.commands.first().operation as AggregateOperation
        assertEquals(
            listOf("${'$'}match", "${'$'}sort"),
            aggregate.pipeline.value().asArray().map { it.asDocument().keys.single() },
        )
        blocked("db.items.aggregate([{a:1,b:2}]);", "PIPELINE_STAGE")
    }

    @Test
    fun expectationsHaveExplicitOwnersAndDoNotAttachByProximity() {
        val request =
            ShellProgramRequest(
                dialect = ShellDialect.MONGOSH,
                blocks =
                    listOf(
                        ProgramBlock(
                            "setup",
                            "{_id:1,v:2}\n{_id:2,v:3}",
                            ProgramRole.SETUP,
                            ProgramFormat.LEGACY_DOCUMENTS,
                            LogicalNamespace(null, "items"),
                        ),
                        ProgramBlock("query", "db.items.find({v:2})", ProgramRole.ACTION),
                        ProgramBlock(
                            "result",
                            "[{_id:NumberInt(1),v:NumberInt(2)}]",
                            ProgramRole.EXPECTED,
                        ),
                    ),
            )
        val result = ShellPrograms.normalize(request)
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        assertEquals(CommandSelector("query", 0), result.expectations.single().command)
        val ambiguous =
            request.copy(
                blocks =
                    request.blocks.toMutableList().apply {
                        add(2, ProgramBlock("second", "db.items.find({v:3})", ProgramRole.ACTION))
                    }
            )
        assertEquals("EXPECTED_OWNER", ShellPrograms.normalize(ambiguous).problems.single().code)
        val explicit =
            ambiguous.copy(
                blocks =
                    ambiguous.blocks.map {
                        if (it.id == "result") it.copy(expectedFor = CommandSelector("query", 0))
                        else it
                    }
            )
        assertEquals(ShellLiteralState.NORMALIZED, ShellPrograms.normalize(explicit).state)
    }

    @Test
    fun harmlessFormattingBindingNamesAndBlockIdsDoNotChangeMeaning() {
        val a = ok("const x=1; db.items.find({v:x});")
        val b =
            ShellPrograms.normalize(
                ShellProgramRequest(
                    dialect = ShellDialect.MONGOSH,
                    blocks =
                        listOf(
                            ProgramBlock(
                                "renamed",
                                "/* intro */ const other = 1; db.items.find( { v : other } ); // EOF",
                                ProgramRole.ACTION,
                            )
                        ),
                )
            )
        assertEquals(ShellLiteralState.NORMALIZED, b.state, b.problems.toString())
        assertEquals(a.semanticSha256, b.semanticSha256)
        assertNotEquals(a.semanticSha256, ok("db.items.find({v:2});").semanticSha256)
    }

    @Test
    fun unknownCommandsMutationsAndConstructorsCannotPublishPartialPlans() {
        for (code in
            listOf(
                "db.items.insertOne({a:1}); db.items.drop();",
                "const x={a:1}; x.a=2; db.items.find(x);",
                "db.runCommand({dropDatabase:1});",
                "const NumberInt=1; db.items.find({a:1});",
                "const cursor=db.items.find({}); cursor.toArray();",
                "for(let x=0;x<2;x++)db.items.insertOne({v:x});",
            )) blocked(code)
        blocked("db.items.find({x:{_bsontype:'Long'}});", "BSON_PROTOCOL_KEY")
    }

    @Test
    fun sourceAndExpansionBudgetsFailWithNoExecutableCommands() {
        val request =
            ShellProgramRequest(
                dialect = ShellDialect.MONGOSH,
                blocks =
                    listOf(
                        ProgramBlock(
                            "a",
                            "let x=[1]; let y=[x,x]; let z=[y,y]; db.items.find({v:z});",
                            ProgramRole.ACTION,
                        )
                    ),
            )
        val limited =
            ShellPrograms.normalize(
                request,
                ShellProgramLimits(values = ShellLiteralLimits(maximumNodes = 8)),
            )
        assertEquals(ShellLiteralState.UNSUPPORTED, limited.state)
        assertTrue(limited.commands.isEmpty())
        val duplicate = request.copy(blocks = request.blocks + request.blocks)
        assertEquals(ShellLiteralState.INVALID, ShellPrograms.normalize(duplicate).state)
        val noAction =
            ShellPrograms.normalize(
                request.copy(
                    blocks = listOf(ProgramBlock("setup", "const x=1;", ProgramRole.SETUP))
                )
            )
        assertEquals("NO_ACTION", noAction.problems.single().code)
    }

    @Test
    fun materializedResultsCannotBeTreatedAsLiveCursors() {
        blocked("db.items.find({}).toArray().limit(1);", "CURSOR_MATERIALIZED")
        blocked("db.items.find({}).toArray().pretty();", "CURSOR_MATERIALIZED")
        ok("db.items.find({}).pretty().limit(1).toArray();")
        blocked("db.items.find({}).project({a:1});", "COLLECTION_REQUIRED")
        ok("db.items.find({}).projection({a:1});")
    }

    @Test
    fun legacySingleAndBatchInsertsRetainTheirDistinctCallShape() {
        val single = ok("db.items.insert({v:1});").commands.single().operation as InsertOperation
        val batch = ok("db.items.insert([{v:1}]);").commands.single().operation as InsertOperation
        assertFalse(single.many)
        assertTrue(batch.many)
    }

    @Test
    fun publishedErrorAssertionsNeedSourceEvidenceAndAnActionOwner() {
        val action =
            ProgramBlock("query", "db.items.aggregate([{\$unknownStage:{}}]);", ProgramRole.ACTION)
        val error =
            ProgramBlock(
                "error",
                "MongoServerError[InvalidPipelineOperator]: code 40324: bad stage",
                ProgramRole.EXPECTED_ERROR,
                ProgramFormat.TEXT,
                expectedError = ProgramExpectedError(40324, "InvalidPipelineOperator", "bad stage"),
            )
        val request =
            ShellProgramRequest(dialect = ShellDialect.MONGOSH, blocks = listOf(action, error))
        val result = ShellPrograms.normalize(request)
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        assertNull(result.expectations.single().documents)
        assertEquals(40324, result.expectations.single().error!!.code)
        val forged =
            request.copy(
                blocks =
                    listOf(
                        action,
                        error.copy(expectedError = ProgramExpectedError(999, null, null)),
                    )
            )
        assertEquals(
            "EXPECTED_ERROR_EVIDENCE",
            ShellPrograms.normalize(forged).problems.single().code,
        )
        val missing = request.copy(blocks = listOf(action, error.copy(expectedError = null)))
        assertEquals(ShellLiteralState.INVALID, ShellPrograms.normalize(missing).state)
    }

    @Test
    fun mixedWriteAndReadActionsNeedAnExplicitExpectationOwner() {
        val actions =
            ProgramBlock(
                "actions",
                "db.items.updateOne({v:1},{\$set:{v:2}}); db.items.find({v:2});",
                ProgramRole.ACTION,
            )
        val result = ProgramBlock("result", "[{v:NumberInt(2)}]", ProgramRole.EXPECTED)
        val request =
            ShellProgramRequest(dialect = ShellDialect.MONGOSH, blocks = listOf(actions, result))
        assertEquals("EXPECTED_OWNER", ShellPrograms.normalize(request).problems.single().code)
        val explicit =
            request.copy(
                blocks = listOf(actions, result.copy(expectedFor = CommandSelector("actions", 1)))
            )
        assertEquals(ShellLiteralState.NORMALIZED, ShellPrograms.normalize(explicit).state)
    }

    @Test
    fun inspectionCanClassifySetupWithoutWeakeningTheExecutableActionContract() {
        val request =
            ShellProgramRequest(
                dialect = ShellDialect.MONGOSH,
                blocks = listOf(ProgramBlock("declaration", "const n=1;", ProgramRole.ACTION)),
            )
        val inspection = ShellPrograms.inspect(request)
        assertEquals(ShellLiteralState.NORMALIZED, inspection.state)
        assertTrue(inspection.operations.isEmpty())
        assertEquals("NO_ACTION", ShellPrograms.normalize(request).problems.single().code)
        val unsafe =
            ShellPrograms.inspect(
                request.copy(
                    blocks = listOf(ProgramBlock("unsafe", "db.items.drop();", ProgramRole.ACTION))
                )
            )
        assertEquals(ShellLiteralState.UNSUPPORTED, unsafe.state)
        assertTrue(unsafe.operations.isEmpty())
    }
}
