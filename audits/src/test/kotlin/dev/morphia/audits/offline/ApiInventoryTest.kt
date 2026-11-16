package dev.morphia.audits.offline

import dev.morphia.audits.api.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ApiInventoryTest {
    private fun capture(root: Path, revision: String) =
        MorphiaApiInventory.capture(root, revision, listOf("."))

    private fun source(root: Path, path: String, content: String): Path =
        root.resolve(path).also {
            it.parent.createDirectories()
            it.writeText(content.trimIndent())
        }

    private val factory =
        """
        package fixture;
        import java.util.List;

        /**
         * @aggregation.expression Expressions
         * @since 2.0
         */
        public class Factories<T extends Number> {
            public static class Expression {}
            public Factories() {}
            /**
             * @aggregation.expression ${'$'}median
             * @mongodb.server.release 7.0
             * @since 3.0
             */
            public static Expression median(Object value) throws IllegalArgumentException { return null; }
            /** @aggregation.expression ${'$'}median */
            public static Expression median(Object value, Expression... additional) { return null; }
            public List<? extends T> values() { return null; }
            private Expression implementationDetail() { return null; }
            private static class Hidden { public void internal() {} }
        }
    """

    @Test
    fun preservesOverloadsAndKeepsClassTagsSeparate(@TempDir root: Path) {
        val input = source(root, "fixture/Factories.java", factory)
        val inventory = capture(root, "revision")
        val owner = inventory.declarations.single { it.id == "fixture.Factories" }
        assertEquals(
            listOf(ApiTag("aggregation.expression", "Expressions"), ApiTag("since", "2.0")),
            owner.tags,
        )
        assertEquals(listOf("T extends java.lang.Number"), owner.typeParameters)
        val overloads = inventory.declarations.filter { it.name == "median" }
        assertEquals(2, overloads.size)
        assertEquals(
            setOf(
                "fixture.Factories#median(java.lang.Object)",
                "fixture.Factories#median(java.lang.Object,fixture.Factories.Expression[])",
            ),
            overloads.map { it.id }.toSet(),
        )
        assertTrue(overloads.all { it.returnType == "fixture.Factories.Expression" })
        assertTrue(overloads.any { it.thrownTypes == listOf("java.lang.IllegalArgumentException") })
        assertEquals(1, overloads.count { it.parameters.last().varargs })
        assertTrue(
            overloads.any {
                ApiTag("mongodb.server.release", "7.0") in it.tags &&
                    ApiTag("since", "3.0") in it.tags
            }
        )
        val helper = inventory.declarations.single { it.name == "values" }
        assertEquals("java.util.List<? extends T>", helper.returnType)
        assertTrue(helper.tags.isEmpty())
        assertTrue(
            inventory.declarations.none {
                it.name == "implementationDetail" || "Hidden" in it.owner
            }
        )
        assertEquals(
            DeclarationKind.CONSTRUCTOR,
            inventory.declarations.single { it.name == "<init>" }.kind,
        )
        assertTrue(
            overloads.all { it.source.path == "fixture/Factories.java" && it.source.line > 1 }
        )
        val bytes = Files.readAllBytes(input)
        val expectedHash =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
        assertEquals(expectedHash, inventory.sourceFiles.single().sha256)
        assertEquals(bytes.size.toLong(), inventory.sourceFiles.single().size)
        assertEquals("revision", inventory.repositoryRevision)
        assertEquals(factory.trimIndent(), Files.readString(input))
        assertEquals(
            listOf(input),
            Files.walk(root).use { it.filter(Files::isRegularFile).toList() },
        )
    }

    @Test
    fun retainsImplicitPublicInterfaceMembersAndNestedTypes(@TempDir root: Path) {
        source(
            root,
            "fixture/Contract.java",
            """
            package fixture;
            /** @query.filter ${'$'}eq */
            public interface Contract {
                String OPTION = "value";
                String call(String... values);
                private void helper() {}
                interface Nested { Thread.State state(); }
            }
        """,
        )
        val declarations = capture(root, "revision").declarations
        assertTrue(declarations.all { it.visibility == "public" })
        assertEquals("INTERFACE", declarations.single { it.id == "fixture.Contract" }.javaKind)
        assertEquals("java.lang.String", declarations.single { it.name == "OPTION" }.returnType)
        assertEquals(
            "java.lang.Thread.State",
            declarations.single { it.name == "state" }.returnType,
        )
        assertTrue(declarations.single { it.name == "call" }.parameters.single().varargs)
        assertTrue(declarations.none { it.name == "helper" })
    }

    @Test
    fun resolvesNestedImportsWildcardsBoundsAndTypeAnnotations(@TempDir root: Path) {
        source(
            root,
            "fixture/Types.java",
            """
            package fixture;
            import java.util.*;
            import java.util.Map;
            import java.lang.annotation.*;
            /** @aggregation.stage ${'$'}project */
            public class Types {
                public <U extends Number & Comparable<U>> U choose(U value) { return null; }
                public Map.Entry<String, ? super Integer>[] entries(List<@Mark String> value) { return null; }
                public void text(@Deprecated(since = "...") String value) {}
            }
            @Target(ElementType.TYPE_USE) @interface Mark {}
        """,
        )
        val declarations = capture(root, "revision").declarations
        assertEquals(
            listOf("U extends java.lang.Number&java.lang.Comparable<U>"),
            declarations.single { it.name == "choose" }.typeParameters,
        )
        val entries = declarations.single { it.name == "entries" }
        assertEquals(
            "java.util.Map.Entry<java.lang.String,? super java.lang.Integer>[]",
            entries.returnType,
        )
        assertEquals("java.util.List<java.lang.String>", entries.parameters.single().type)
        val text = declarations.single { it.name == "text" }
        assertFalse(text.parameters.single().varargs)
        assertTrue(text.parameters.single().annotations.single().contains("..."))
    }

    @Test
    fun outputIsIndependentOfRootAndCreationOrder(@TempDir root: Path) {
        val first = root.resolve("one").createDirectories()
        val second = root.resolve("two").createDirectories()
        val extra = "package fixture; public class Extra { public int count() { return 0; } }"
        source(first, "fixture/Factories.java", factory)
        source(first, "fixture/Extra.java", extra)
        source(second, "fixture/Extra.java", extra)
        source(second, "fixture/Factories.java", factory)
        val before = capture(first, "revision").toJson()
        assertEquals(before, capture(second, "revision").toJson())
        assertEquals(before, capture(first, "revision").toJson())
        assertFalse(before.contains(first.toString()))
    }

    @Test
    fun importChangesAffectQualifiedSignatureAndSourceHash(@TempDir root: Path) {
        val input =
            source(
                root,
                "fixture/Dates.java",
                """
            package fixture;
            import java.util.Date;
            /** @update.operator ${'$'}currentDate */
            public class Dates { public void date(Date value) {} }
        """,
            )
        val before = capture(root, "revision")
        input.writeText(Files.readString(input).replace("java.util.Date", "java.sql.Date"))
        val after = capture(root, "revision")
        assertNotEquals(before.sourceFiles.single().sha256, after.sourceFiles.single().sha256)
        assertEquals(
            "fixture.Dates#date(java.util.Date)",
            before.declarations.single { it.name == "date" }.id,
        )
        assertEquals(
            "fixture.Dates#date(java.sql.Date)",
            after.declarations.single { it.name == "date" }.id,
        )
    }

    @Test
    fun recordsExplicitDependencyRootsAndRejectsOverlappingOrEscapingRoots(@TempDir root: Path) {
        source(
            root,
            "core/fixture/Factories.java",
            factory.replace("T extends Number", "T extends Shared"),
        )
        source(root, "annotations/fixture/Shared.java", "package fixture; public class Shared {}")
        val inventory = MorphiaApiInventory.capture(root, "revision", listOf("core", "annotations"))
        assertEquals(listOf("annotations", "core"), inventory.sourceRoots)
        assertEquals(
            "T extends fixture.Shared",
            inventory.declarations.single { it.id == "fixture.Factories" }.typeParameters.single(),
        )
        assertEquals(2, inventory.sourceFiles.size)
        assertThrows(IllegalArgumentException::class.java) {
            MorphiaApiInventory.capture(root, "revision", emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            MorphiaApiInventory.capture(root, "revision", listOf("core", "core/fixture"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            MorphiaApiInventory.capture(root, "revision", listOf(".."))
        }
        assertThrows(IllegalArgumentException::class.java) {
            MorphiaApiInventory.capture(root, "revision", listOf(root.toString()))
        }
    }

    @Test
    fun rejectsWrongEmptyAndUntaggedRoots(@TempDir root: Path) {
        assertThrows(java.nio.file.NoSuchFileException::class.java) {
            capture(root.resolve("missing"), "revision")
        }
        assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
        source(root, "Plain.java", "public class Plain { public int count() { return 0; } }")
        val error = assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
        assertTrue(error.message!!.contains("No public operator-tagged declarations"))
        assertThrows(IllegalArgumentException::class.java) { capture(root, " ") }
    }

    @Test
    fun rejectsInvalidSourceEncodingAndDuplicateDeclarations(@TempDir root: Path) {
        val file = source(root, "Broken.java", "public class Broken { public void broken( }")
        val syntax =
            assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
        assertTrue(syntax.message!!.contains("Invalid Java source"))
        Files.write(file, byteArrayOf(0xC3.toByte(), 0x28))
        assertThrows(java.nio.charset.CharacterCodingException::class.java) {
            capture(root, "revision")
        }
        file.writeText(
            "/** @query.filter ${'$'}eq */ public class Broken { public void call() {} public void call() {} }"
        )
        assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
    }

    @Test
    fun unresolvedAndAmbiguousTypesFailWithSourceLocation(@TempDir root: Path) {
        val file =
            source(
                root,
                "fixture/Unknown.java",
                """
            package fixture;
            /** @aggregation.expression ${'$'}unknown */
            public class Unknown { public Missing value() { return null; } }
        """,
            )
        val unknown =
            assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
        assertTrue(unknown.message!!.contains("fixture/Unknown.java:3:"))
        assertTrue(unknown.message!!.contains("Missing"))
        file.writeText(
            Files.readString(file)
                .replace(
                    "package fixture;",
                    "package fixture; import java.util.*; import java.sql.*;",
                )
                .replace("Missing", "Date")
        )
        val ambiguous =
            assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
        assertTrue(
            ambiguous.message!!.contains("java.util.Date") &&
                ambiguous.message!!.contains("java.sql.Date")
        )
    }

    @Test
    fun rejectsSymlinkedInputs(@TempDir root: Path) {
        val actual = root.resolve("actual").createDirectories()
        val input = source(actual, "fixture/Factories.java", factory)
        val linked = root.resolve("linked").createDirectories()
        Files.createSymbolicLink(linked.resolve("Factories.java"), input)
        assertThrows(IllegalArgumentException::class.java) { capture(linked, "revision") }
        Files.delete(linked.resolve("Factories.java"))
        Files.createSymbolicLink(linked.resolve("nested"), actual)
        assertThrows(IllegalArgumentException::class.java) { capture(linked, "revision") }
    }

    @Test
    fun retainsRecordComponentsEnumConstantsAndAnnotationMembers(@TempDir root: Path) {
        source(
            root,
            "fixture/Shapes.java",
            """
            package fixture;
            /** @aggregation.expression ${'$'}shape */
            public class Shapes {
                /**
                 * @param name the display name
                 * @param values the values
                 */
                public record Result(String name, Object... values) {
                    private static final int INTERNAL = 1;
                }
                public enum Choice { FIRST, SECOND }
                public @interface Label { String value() default "example"; }
                public sealed interface Shape permits Circle {}
                public static final class Circle implements Shape {}
                protected void hidden() {}
                void packageOnly() {}
            }
        """,
        )
        val inventory = capture(root, "revision")
        val record = inventory.declarations.single { it.id == "fixture.Shapes.Result" }
        assertEquals("RECORD", record.javaKind)
        assertEquals(listOf("name", "values"), record.recordComponents.map { it.name })
        assertEquals(
            listOf("java.lang.String", "java.lang.Object[]"),
            record.recordComponents.map { it.type },
        )
        assertTrue(ApiTag("param", "name the display name") in record.tags)
        assertEquals(
            setOf("FIRST", "SECOND"),
            inventory.declarations
                .filter { it.owner == "fixture.Shapes.Choice" && it.kind == DeclarationKind.FIELD }
                .map { it.name }
                .toSet(),
        )
        assertEquals(
            "ANNOTATION_TYPE",
            inventory.declarations.single { it.id == "fixture.Shapes.Label" }.javaKind,
        )
        assertEquals(
            "java.lang.String",
            inventory.declarations.single { it.id == "fixture.Shapes.Label#value()" }.returnType,
        )
        assertTrue(
            inventory.declarations.none { it.name in setOf("INTERNAL", "hidden", "packageOnly") }
        )
        assertTrue(inventory.parser.runtimeVersion.isNotBlank())
        assertEquals("jdk-javac", inventory.parser.engine)
        assertEquals(
            listOf("fixture.Shapes.Circle"),
            inventory.declarations.single { it.id == "fixture.Shapes.Shape" }.permittedSubtypes,
        )
        assertEquals(
            "\"example\"",
            inventory.declarations
                .single { it.id == "fixture.Shapes.Label#value()" }
                .annotationDefault,
        )
    }

    @Test
    fun inheritedMemberTypesTakePrecedenceOverPackageTypes(@TempDir root: Path) {
        source(
            root,
            "fixture/Builder.java",
            "package fixture; public class Builder { public static class Leaf {} }",
        )
        source(
            root,
            "fixture/Parent.java",
            "package fixture; public class Parent { public static class Builder { public static class Leaf {} } }",
        )
        val child =
            source(
                root,
                "fixture/Child.java",
                """
            package fixture;
            /** @aggregation.stage ${'$'}build */
            public class Child extends Parent { public Builder build() { return null; } }
        """,
            )
        source(
            root,
            "fixture/Outer.java",
            "package fixture; public class Outer extends Parent { public static class Nested extends Builder { public Leaf leaf() { return null; } } }",
        )
        val inventory = capture(root, "revision")
        assertEquals(
            listOf("fixture.Parent.Builder"),
            inventory.declarations.single { it.id == "fixture.Outer.Nested" }.supertypes,
        )
        assertEquals(
            "fixture.Parent.Builder.Leaf",
            inventory.declarations.single { it.name == "leaf" }.returnType,
        )
        val method = inventory.declarations.single { it.name == "build" }
        assertEquals("fixture.Parent.Builder", method.returnType)
        child.writeText(
            """
            package fixture;
            interface Left { class Builder {} }
            interface Right { class Builder {} }
            /** @aggregation.stage ${'$'}build */
            public class Child implements Left, Right { public Builder build() { return null; } }
            """
                .trimIndent()
        )
        val ambiguous =
            assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
        assertTrue(ambiguous.message!!.contains("ambiguous"))
    }

    @Test
    fun missingDependenciesFailWithoutUsingAmbientClasspath(@TempDir root: Path) {
        source(
            root,
            "fixture/Unknown.java",
            """
            package fixture;
            import external.Base;
            /** @aggregation.stage ${'$'}unknown */
            public class Unknown extends Base { public String value() { return null; } }
        """,
        )
        val error = assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
        assertTrue(error.message!!.contains("incomplete explicit classpath"))
        assertTrue(error.message!!.contains("external"))
    }

    @Test
    fun erasedOverloadIdentitySurvivesTypeVariableRenaming(@TempDir root: Path) {
        val file =
            source(
                root,
                "fixture/Generic.java",
                """
            package fixture;
            import java.util.List;
            /** @query.filter ${'$'}eq */
            public class Generic<T extends Number> { public void call(T value, List<String> other) {} }
        """,
            )
        val before = capture(root, "revision")
        file.writeText(
            Files.readString(file).replace("<T extends", "<U extends").replace("call(T ", "call(U ")
        )
        val after = capture(root, "revision")
        assertEquals(before.declarations.map { it.id }, after.declarations.map { it.id })
        assertEquals(
            "fixture.Generic#call(java.lang.Number,java.util.List)",
            after.declarations.single { it.name == "call" }.id,
        )
        assertNotEquals(before.sourceFiles.single().sha256, after.sourceFiles.single().sha256)
        file.writeText(
            Files.readString(file)
                .replace(
                    "public void call",
                    "public void call(U value, List<Integer> other) {} public void call",
                )
        )
        val duplicate =
            assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
        assertTrue(duplicate.message!!.contains("same erasure"))
    }

    @Test
    fun emptySourceRootsFailAndMalformedJavadocIsRetained(@TempDir root: Path) {
        source(
            root,
            "code/Example.java",
            """
            /** @query.filter {@link */
            public class Example { public void call() {} }
        """,
        )
        root.resolve("empty").createDirectories()
        assertThrows(IllegalArgumentException::class.java) {
            MorphiaApiInventory.capture(root, "revision", listOf("code", "empty"))
        }
        val inventory = MorphiaApiInventory.capture(root, "revision", listOf("code"))
        assertTrue(
            inventory.declarations
                .single { it.kind == DeclarationKind.TYPE }
                .documentationErrors
                .isNotEmpty()
        )
        assertTrue(
            inventory.declarations
                .single { it.kind == DeclarationKind.TYPE }
                .tags
                .single()
                .value
                .contains("{@link")
        )
        assertEquals("", inventory.sourceUnits.single().packageName)
    }

    @Test
    fun compactRecordConstructorsRetainCanonicalParameters(@TempDir root: Path) {
        source(
            root,
            "fixture/Result.java",
            """
            package fixture;
            /** @aggregation.expression ${'$'}result */
            public record Result(String name, Object... values) {
                public Result { if (name == null) throw new IllegalArgumentException(); }
                public Result() { this("default"); }
            }
        """,
        )
        val constructors =
            capture(root, "revision").declarations.filter { it.kind == DeclarationKind.CONSTRUCTOR }
        assertEquals(
            setOf(
                "fixture.Result#<init>()",
                "fixture.Result#<init>(java.lang.String,java.lang.Object[])",
            ),
            constructors.map { it.id }.toSet(),
        )
        assertTrue(constructors.single { it.parameters.isNotEmpty() }.parameters.last().varargs)
    }

    @Test
    fun dottedExternalNamesAndPackageShadowingRemainExplicit(@TempDir root: Path) {
        val input =
            source(
                root,
                "fixture/External.java",
                """
            package fixture;
            /** @aggregation.expression ${'$'}external */
            public class External {
                public com.vendor.Expression expression() { return null; }
            }
        """,
            )
        source(root, "com/vendor/Expression.java", "package com.vendor; public class Expression {}")
        val external = capture(root, "revision").declarations.single { it.name == "expression" }
        assertEquals("com.vendor.Expression", external.returnType)
        input.writeText(
            Files.readString(input)
                .replace(
                    "public com.vendor.Expression",
                    "public static class com { public static class vendor { public static class Expression {} } } public com.vendor.Expression",
                )
        )
        val shadowed = capture(root, "revision").declarations.single { it.name == "expression" }
        assertEquals("fixture.External.com.vendor.Expression", shadowed.returnType)
    }

    @Test
    fun explicitClasspathIsHashedWithoutLoadingOrExecutingSource(@TempDir root: Path) {
        val marker = root.resolve("executed")
        val classes = root.resolve("dependencies").createDirectories()
        val library =
            source(
                root,
                "library/external/Expression.java",
                """
            package external;
            public class Expression {
                public static final int VERSION = 1;
                static { try { java.nio.file.Files.writeString(java.nio.file.Path.of("${marker.toString().replace("\\", "\\\\").replace("\"", "\\\"")}"), "executed"); } catch (Exception e) { throw new RuntimeException(e); } }
            }
        """,
            )
        val compiler = javax.tools.ToolProvider.getSystemJavaCompiler()
        assertEquals(
            0,
            compiler.run(
                null,
                null,
                null,
                "-proc:none",
                "-d",
                classes.toString(),
                library.toString(),
            ),
        )
        source(
            root,
            "code/fixture/Factory.java",
            """
            package fixture;
            /** @aggregation.expression ${'$'}factory */
            public class Factory { public external.Expression create() { return new external.Expression(); } }
        """,
        )
        val before = MorphiaApiInventory.capture(root, "revision", listOf("code"), listOf(classes))
        assertEquals(
            "external.Expression",
            before.declarations.single { it.name == "create" }.returnType,
        )
        assertEquals(1, before.sourceFiles.size)
        assertEquals("dependencies", before.classpath.single().name)
        assertFalse(Files.exists(marker))
        assertTrue(
            Files.walk(root.resolve("code")).use { files ->
                files.noneMatch { it.toString().endsWith(".class") }
            }
        )
        library.writeText(Files.readString(library).replace("VERSION = 1", "VERSION = 2"))
        assertEquals(
            0,
            compiler.run(
                null,
                null,
                null,
                "-proc:none",
                "-d",
                classes.toString(),
                library.toString(),
            ),
        )
        val after = MorphiaApiInventory.capture(root, "revision", listOf("code"), listOf(classes))
        assertNotEquals(before.classpath.single().sha256, after.classpath.single().sha256)
        assertEquals(before.declarations.map { it.id }, after.declarations.map { it.id })
        assertFalse(Files.exists(marker))
    }

    @Test
    fun compilerScopesBoundsAtTheirDeclarationAndCanonicalizesInheritedAliases(
        @TempDir root: Path
    ) {
        source(
            root,
            "fixture/Scope.java",
            """
            package fixture;
            /** @aggregation.expression ${'$'}scope */
            public class Scope<T extends Number> {
                public static class Number {}
                public class Inner {
                    public static class Number {}
                    public T value(T value) { return null; }
                    public <Number> void put(T value) {}
                }
            }
        """,
        )
        source(
            root,
            "fixture/Parent.java",
            "package fixture; public class Parent { public static class Builder {} }",
        )
        val child =
            source(
                root,
                "fixture/Child.java",
                "package fixture; public class Child extends Parent { public void bind(Parent.Builder value) {} }",
            )
        val before = capture(root, "revision")
        assertEquals(
            listOf("T extends java.lang.Number"),
            before.declarations.single { it.id == "fixture.Scope" }.typeParameters,
        )
        assertEquals(
            "fixture.Scope.Inner#value(java.lang.Number)",
            before.declarations.single { it.name == "value" }.id,
        )
        assertEquals(
            "fixture.Scope.Inner#put(java.lang.Number)",
            before.declarations.single { it.name == "put" }.id,
        )
        child.writeText(
            Files.readString(child).replace("Parent.Builder value", "Child.Builder value")
        )
        val after = capture(root, "revision")
        assertEquals(before.declarations.map { it.id }, after.declarations.map { it.id })
    }

    @Test
    fun rejectsQualifiedTyposAndDoesNotUseTheTestRuntimeClasspath(@TempDir root: Path) {
        val file =
            source(
                root,
                "fixture/Invalid.java",
                """
            package fixture;
            /** @query.filter ${'$'}invalid */
            public class Invalid { public Thread.Stat value() { return null; } }
        """,
            )
        assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
        file.writeText(Files.readString(file).replace("Thread.Stat", "org.junit.jupiter.api.Test"))
        assertThrows(IllegalArgumentException::class.java) { capture(root, "revision") }
    }

    @Test
    fun rejectsArchivesWithUnrecordedManifestDependencies(@TempDir root: Path) {
        source(root, "Example.java", "/** @query.filter ${'$'}eq */ public class Example {}")
        val archive = root.resolve("manifest.jar")
        val manifest = java.util.jar.Manifest()
        manifest.mainAttributes[java.util.jar.Attributes.Name.MANIFEST_VERSION] = "1.0"
        manifest.mainAttributes[java.util.jar.Attributes.Name.CLASS_PATH] = "neighbor.jar"
        java.util.jar.JarOutputStream(Files.newOutputStream(archive), manifest).use {}
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                MorphiaApiInventory.capture(root, "revision", listOf("."), listOf(archive))
            }
        assertTrue(error.message!!.contains("manifest Class-Path"))
    }

    @Test
    fun cliEmitsUtf8EvenWhenThePlatformCharsetIsAscii(@TempDir root: Path) {
        source(root, "Example.java", "/** @query.filter ${'$'}eq Café */ public class Example {}")
        val javaName =
            if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java"
        val javaExecutable = Path.of(System.getProperty("java.home"), "bin", javaName)
        val errors = root.resolve("stderr.txt")
        val dependencies = root.resolve("dependency/classes").createDirectories()
        val process =
            ProcessBuilder(
                    javaExecutable.toString(),
                    "-Dfile.encoding=US-ASCII",
                    "-Dsun.stdout.encoding=US-ASCII",
                    "-cp",
                    System.getProperty("java.class.path"),
                    "dev.morphia.audits.api.ApiInventoryMainKt",
                    root.toString(),
                    "revision",
                    "--classpath-entry",
                    dependencies.toString(),
                    ".",
                )
                .redirectError(errors.toFile())
                .start()
        try {
            assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS))
            assertEquals(0, process.exitValue(), Files.readString(errors))
            val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)
            assertTrue(output.contains("Café"), output)
            assertEquals(
                "classes",
                com.fasterxml.jackson.databind
                    .ObjectMapper()
                    .readTree(output)
                    .path("classpath")
                    .get(0)
                    .path("name")
                    .asText(),
            )
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }
}
