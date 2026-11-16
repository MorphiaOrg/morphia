package dev.morphia.audits.api

import com.sun.source.doctree.*
import com.sun.source.tree.*
import com.sun.source.util.DocTreeScanner
import com.sun.source.util.DocTrees
import com.sun.source.util.JavacTask
import com.sun.source.util.TreePath
import dev.morphia.audits.CorpusFile
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Locale
import javax.lang.model.element.*
import javax.lang.model.type.TypeKind
import javax.tools.*

/** Compiler analysis only: processors and implicit source discovery are disabled; no generation. */
object MorphiaApiInventory {
    fun capture(
        repository: Path,
        revision: String,
        sourceDirectories: List<String>,
        classpath: List<Path> = emptyList(),
    ): ApiInventory {
        require(revision.isNotBlank()) { "A repository revision is required" }
        val root = repository.toRealPath()
        require(Files.isDirectory(root)) { "Not a repository directory: $root" }
        require(sourceDirectories.isNotEmpty()) {
            "At least one explicit Java source directory is required"
        }
        val directories =
            sourceDirectories.map { value ->
                require(!Path.of(value).isAbsolute) {
                    "Source directories must be repository-relative: $value"
                }
                val directory = root.resolve(value).normalize()
                require(directory.startsWith(root)) {
                    "Source directory escapes repository: $value"
                }
                require(Files.isDirectory(directory) && directory.toRealPath() == directory) {
                    "Missing or symlinked source directory: $value"
                }
                directory
            }
        require(
            directories.withIndex().none { (index, directory) ->
                directories.take(index).any { directory.startsWith(it) || it.startsWith(directory) }
            }
        ) {
            "Duplicate or overlapping source directories"
        }
        val paths =
            directories
                .flatMap { directory ->
                    Files.walk(directory)
                        .use { entries ->
                            entries
                                .sorted()
                                .toList()
                                .onEach {
                                    require(!Files.isSymbolicLink(it)) {
                                        "Source entries must not be symlinks: $it"
                                    }
                                }
                                .filter {
                                    Files.isRegularFile(it) &&
                                        it.fileName.toString().endsWith(".java")
                                }
                        }
                        .also { require(it.isNotEmpty()) { "No Java sources in $directory" } }
                }
                .sorted()
        val inputs =
            paths.map { path ->
                val bytes = Files.readAllBytes(path)
                val text = bytes.decodeToString(throwOnInvalidSequence = true)
                val file =
                    object : SimpleJavaFileObject(path.toUri(), JavaFileObject.Kind.SOURCE) {
                        override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence =
                            text
                    }
                val hash =
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                Pair(
                    file,
                    CorpusFile(root.relativize(path).joinToString("/"), hash, bytes.size.toLong()),
                )
            }
        val classPaths = classpath.map { it.toRealPath() }
        val classpathInputs =
            classPaths.mapIndexed { index, path -> SourceClasspath.digest(path, index) }
        val compiler =
            requireNotNull(ToolProvider.getSystemJavaCompiler()) {
                "Source inventory requires a full JDK 17 or newer"
            }
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        fun checkErrors() {
            val errors = diagnostics.diagnostics.filter { it.kind == Diagnostic.Kind.ERROR }
            require(errors.isEmpty()) {
                "Invalid Java source or incomplete explicit classpath: " +
                    errors.joinToString("; ") {
                        "${it.source?.name}:${it.lineNumber}: ${it.getMessage(Locale.ROOT)}"
                    }
            }
        }
        compiler.getStandardFileManager(diagnostics, Locale.ROOT, UTF_8).use { manager ->
            manager.setLocationFromPaths(StandardLocation.CLASS_PATH, classPaths)
            manager.setLocationFromPaths(StandardLocation.SOURCE_PATH, emptyList())
            manager.setLocationFromPaths(StandardLocation.ANNOTATION_PROCESSOR_PATH, emptyList())
            val task =
                compiler.getTask(
                    null,
                    manager,
                    diagnostics,
                    listOf("-proc:none", "--release", "17", "-implicit:none"),
                    null,
                    inputs.map { it.first },
                ) as JavacTask
            val units = task.parse().toList()
            checkErrors()
            val trees = DocTrees.instance(task)
            val raw = mutableListOf<RawDeclaration>()
            units.forEach { unit ->
                val path = root.relativize(Path.of(unit.sourceFile.toUri())).joinToString("/")
                fun collect(
                    node: Tree,
                    kind: DeclarationKind,
                    owner: String,
                    name: String,
                    modifiers: ModifiersTree,
                    parameters: List<VariableTree> = emptyList(),
                    returnType: Tree? = null,
                ) {
                    val treePath = TreePath.getPath(unit, node)
                    val documentation = trees.getDocCommentTree(treePath)
                    val errors = mutableListOf<String>()
                    object : DocTreeScanner<Unit, Unit>() {
                            override fun visitErroneous(
                                node: com.sun.source.doctree.ErroneousTree,
                                unused: Unit,
                            ) {
                                errors += node.diagnostic.getMessage(Locale.ROOT)
                            }
                        }
                        .scan(documentation, Unit)
                    val tags =
                        documentation?.blockTags.orEmpty().map { tag ->
                            val name = (tag as? BlockTagTree)?.tagName ?: "erroneous"
                            val value =
                                when (tag) {
                                    is UnknownBlockTagTree -> tag.content.joinToString("")
                                    is SinceTree -> tag.body.joinToString("")
                                    is DeprecatedTree -> tag.body.joinToString("")
                                    else ->
                                        if (tag is BlockTagTree)
                                            tag.toString().removePrefix("@$name").trimStart()
                                        else tag.toString()
                                }
                            ApiTag(name, value)
                        }
                    fun parameter(value: VariableTree) =
                        RawParameter(
                            value.type.toString(),
                            value.modifiers.annotations.map { it.toString() },
                        )
                    raw +=
                        RawDeclaration(
                            treePath,
                            kind,
                            node.kind.name,
                            owner,
                            name,
                            modifiers.flags.map { it.toString() }.sorted(),
                            modifiers.annotations.map { it.toString() },
                            parameters.associate { it.name.toString() to parameter(it) },
                            returnType?.toString(),
                            if (node is ClassTree && node.kind == Tree.Kind.RECORD)
                                node.members
                                    .filterIsInstance<VariableTree>()
                                    .filter { Modifier.STATIC !in it.modifiers.flags }
                                    .associate { it.name.toString() to parameter(it) }
                            else emptyMap(),
                            tags,
                            errors,
                            SourceLocation(
                                path,
                                unit.lineMap.getLineNumber(
                                    trees.sourcePositions.getStartPosition(unit, node)
                                ),
                            ),
                        )
                }
                fun visit(tree: ClassTree, prefix: String, implicitPublic: Boolean) {
                    if (Modifier.PUBLIC !in tree.modifiers.flags && !implicitPublic) return
                    val owner =
                        if (prefix.isEmpty()) tree.simpleName.toString()
                        else "$prefix.${tree.simpleName}"
                    val interfaceType =
                        tree.kind in setOf(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)
                    collect(
                        tree,
                        DeclarationKind.TYPE,
                        owner,
                        tree.simpleName.toString(),
                        tree.modifiers,
                    )
                    // Freeze declarations before analyze() inserts synthesized
                    // constructors/methods.
                    tree.members.toList().forEach { member ->
                        when (member) {
                            is ClassTree -> visit(member, owner, interfaceType)
                            is MethodTree ->
                                if (
                                    Modifier.PUBLIC in member.modifiers.flags ||
                                        (interfaceType &&
                                            Modifier.PRIVATE !in member.modifiers.flags)
                                ) {
                                    collect(
                                        member,
                                        if (member.returnType == null) DeclarationKind.CONSTRUCTOR
                                        else DeclarationKind.METHOD,
                                        owner,
                                        member.name.toString(),
                                        member.modifiers,
                                        member.parameters.toList(),
                                        member.returnType,
                                    )
                                }
                            is VariableTree ->
                                if (Modifier.PUBLIC in member.modifiers.flags || interfaceType)
                                    collect(
                                        member,
                                        DeclarationKind.FIELD,
                                        owner,
                                        member.name.toString(),
                                        member.modifiers,
                                        returnType = member.type,
                                    )
                        }
                    }
                }
                unit.typeDecls.filterIsInstance<ClassTree>().forEach {
                    visit(it, unit.packageName?.toString().orEmpty(), false)
                }
            }
            task.analyze().toList()
            checkErrors()
            require(
                classPaths.mapIndexed { index, path -> SourceClasspath.digest(path, index) } ==
                    classpathInputs
            ) {
                "Classpath changed during source analysis"
            }
            val declarations =
                raw.map { declaration ->
                    val element =
                        requireNotNull(trees.getElement(declaration.path)) {
                            "Missing compiler element at ${declaration.source}"
                        }
                    require(Modifier.PUBLIC in element.modifiers) {
                        "Nonpublic declaration at ${declaration.source}"
                    }
                    val method = element as? ExecutableElement
                    val type = element as? TypeElement
                    val parameters =
                        method?.parameters.orEmpty().mapIndexed { index, parameter ->
                            val original =
                                requireNotNull(
                                    declaration.parameters[parameter.simpleName.toString()]
                                ) {
                                    "Missing declared parameter: $parameter"
                                }
                            ApiParameter(
                                parameter.simpleName.toString(),
                                CompilerTypes.name(parameter.asType()),
                                original.type,
                                original.annotations,
                                method!!.isVarArgs && index == method.parameters.lastIndex,
                            )
                        }
                    val id =
                        when (declaration.kind) {
                            DeclarationKind.TYPE -> declaration.owner
                            DeclarationKind.FIELD -> "${declaration.owner}#${declaration.name}"
                            else ->
                                "${declaration.owner}#${declaration.name}" +
                                    method!!.parameters.joinToString(",", "(", ")") {
                                        CompilerTypes.name(task.types.erasure(it.asType()))
                                    }
                        }
                    val typeParameters =
                        (method?.typeParameters ?: type?.typeParameters).orEmpty().map { parameter
                            ->
                            parameter.simpleName.toString() +
                                parameter.bounds.joinToString(
                                    "&",
                                    " extends ",
                                    transform = CompilerTypes::name,
                                )
                        }
                    ApiDeclaration(
                        id = id,
                        kind = declaration.kind,
                        javaKind = declaration.javaKind,
                        owner = declaration.owner,
                        name = declaration.name,
                        visibility = "public",
                        parserModifiers = declaration.modifiers,
                        modifiers = element.modifiers.map { it.toString() }.sorted(),
                        annotations = declaration.annotations,
                        typeParameters = typeParameters,
                        parameters = parameters,
                        returnType =
                            when {
                                declaration.kind == DeclarationKind.CONSTRUCTOR -> null
                                method != null -> CompilerTypes.name(method.returnType)
                                element is VariableElement -> CompilerTypes.name(element.asType())
                                else -> null
                            },
                        declaredReturnType = declaration.returnType,
                        thrownTypes = method?.thrownTypes.orEmpty().map(CompilerTypes::name),
                        supertypes =
                            if (type == null) emptyList()
                            else
                                (listOf(type.superclass).filter { it.kind != TypeKind.NONE } +
                                        type.interfaces)
                                    .map(CompilerTypes::name),
                        permittedSubtypes =
                            type?.permittedSubclasses.orEmpty().map(CompilerTypes::name),
                        annotationDefault = method?.defaultValue?.toString(),
                        tags = declaration.tags,
                        documentationErrors = declaration.documentationErrors,
                        recordComponents =
                            type?.recordComponents.orEmpty().map { component ->
                                val original =
                                    requireNotNull(
                                        declaration.components[component.simpleName.toString()]
                                    )
                                ApiRecordComponent(
                                    component.simpleName.toString(),
                                    CompilerTypes.name(component.asType()),
                                    original.type,
                                    original.annotations,
                                )
                            },
                        source = declaration.source,
                    )
                }
            val duplicateIds = declarations.groupBy { it.id }.filterValues { it.size > 1 }
            require(duplicateIds.isEmpty()) {
                "Duplicate public source declarations: $duplicateIds"
            }
            require(declarations.any { d -> d.tags.any { it.name in ApiInventory.operatorTags } }) {
                "No public operator-tagged declarations in $root; inventory is invalid"
            }
            return ApiInventory(
                repositoryRevision = revision,
                sourceRoots =
                    directories
                        .map { root.relativize(it).joinToString("/").ifEmpty { "." } }
                        .sorted(),
                sourceFiles = inputs.map { it.second },
                classpath = classpathInputs,
                sourceUnits =
                    units
                        .map {
                            ApiSourceUnit(
                                root.relativize(Path.of(it.sourceFile.toUri())).joinToString("/"),
                                it.packageName?.toString().orEmpty(),
                                it.imports.map { import -> import.toString().trim() },
                            )
                        }
                        .sortedBy { it.path },
                declarations = declarations.sortedBy { it.id },
            )
        }
    }

    private data class RawParameter(val type: String, val annotations: List<String>)

    private data class RawDeclaration(
        val path: TreePath,
        val kind: DeclarationKind,
        val javaKind: String,
        val owner: String,
        val name: String,
        val modifiers: List<String>,
        val annotations: List<String>,
        val parameters: Map<String, RawParameter>,
        val returnType: String?,
        val components: Map<String, RawParameter>,
        val tags: List<ApiTag>,
        val documentationErrors: List<String>,
        val source: SourceLocation,
    )
}
