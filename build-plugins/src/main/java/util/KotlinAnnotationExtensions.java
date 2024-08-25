package util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.squareup.kotlinpoet.AnnotationSpec;
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget;
import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.FileSpec;
import com.squareup.kotlinpoet.FunSpec;
import com.squareup.kotlinpoet.TypeSpec;
import com.squareup.kotlinpoet.TypeSpec.Builder;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jboss.forge.roaster.ParserException;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.AnnotationElementSource;
import org.jboss.forge.roaster.model.source.JavaAnnotationSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.util.Types;
import org.jetbrains.annotations.NotNull;

import kotlin.Suppress;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static util.AnnotationBuilders.methodCase;

@Mojo(name = "morphia-annotations-kotlin", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class KotlinAnnotationExtensions extends AbstractMojo {

    private Map<String, JavaAnnotationSource> builders = new TreeMap<>();

    private FileSpec.Builder fileBuilder;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private Builder factory;

    private File generated;

    private final FileFilter filter = pathname -> (pathname.getName().endsWith(".java") || pathname.getName().endsWith(".kt"))
            && !pathname.getName().endsWith("Handler.java")
            && !pathname.getName().endsWith("Helper.java")
            && !pathname.getName().equals("package-info.java");

    @Override
    @SuppressWarnings("ConstantConditions")
    public void execute() throws MojoExecutionException {
        List<File> files = new ArrayList<>();
        generated = new File(project.getBasedir() + "/target/generated-sources/morphia-annotations/");

        String path = core() + "/src/main/java/dev/morphia/annotations";
        files.addAll(find(path));
        project.addCompileSourceRoot(generated.getAbsolutePath());

        try {
            for (File file : files) {
                try {
                    var source = Roaster.parse(JavaAnnotationSource.class, file);
                    if (source.isPublic()) {
                        builders.put(source.getName(), source);
                    }
                } catch (ParserException e) {
                    throw new MojoExecutionException("Could not parse " + file, e);
                }
            }
            fileBuilder = FileSpec.builder(builders.values().iterator().next().getPackage() + ".internal", "AnnotationKspFactory");
            fileBuilder.addAnnotation(AnnotationSpec.builder(Suppress.class)
                    .addMember("%S", "UNCHECKED_CAST")
                    .useSiteTarget(UseSiteTarget.FILE)
                    .build());

            fileBuilder.addImport("dev.morphia.critter.parser.ksp.extensions",
                    "allAnnotations", "name", "className");
            fileBuilder.addImport("dev.morphia.mapping", "MappingException");
            fileBuilder.addImport("java.util", "Objects");
            emitFactory();
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private File core() {
        var dir = project.getBasedir();
        while (!new File(dir, ".git").exists()) {
            dir = dir.getParentFile();
        }
        return new File(dir, "core");
    }

    private void emitFactory() throws Exception {
        if (factory == null) {
            factory = createFactory();
            for (JavaAnnotationSource source : builders.values()) {
                annotationConverters(source);
                annotationExtractors(source);
                annotationCodeBuilders(source);
            }
        }

        fileBuilder.addType(factory.build());

        FileSpec fileSpec = fileBuilder.build();
        var outputFile = new File(generated, fileSpec.getRelativePath());
        if (!outputFile.getParentFile().mkdirs() && !outputFile.getParentFile().exists()) {
            throw new IOException(format("Could not create directory: %s", outputFile.getParentFile()));
        }
        try (var out = new FileWriter(outputFile)) {
            fileSpec.writeTo(out);
        }
    }

    private void annotationExtractors(JavaAnnotationSource source) {
        var method = FunSpec.builder("%sAnnotation".formatted(methodCase(source.getName())))
                .receiver(KSClassDeclaration.class)
                .returns(ClassName.bestGuess(source.getQualifiedName()));

        method.addCode("""
                    return try {
                        allAnnotations()
                            .first { it.annotationType.className() == %L::class.java.name }
                            .to%L()
                    } catch (e: NoSuchElementException) {
                        throw MappingException("No Entity annotation found on ${name()}")
                    }
                """, source.getName(), source.getName());

        factory.addFunction(method.build());
    }

    private void annotationConverters(JavaAnnotationSource source) {
        var method = FunSpec.builder("to" + source.getName())
                .receiver(KSAnnotation.class)
                .returns(ClassName.bestGuess(source.getQualifiedName()));

        var code = MessageFormat.format("""
                val map = arguments
                    .map '{' it -> (it.name?.asString() ?: "value") to it.value }
                    .toMap()
                var builder = {0}Builder.{1}Builder().apply '{'
                """,
                source.getName(), methodCase(source.getName()));

        for (AnnotationElementSource element : source.getAnnotationElements()) {
            String name = element.getName();
            String cast = processType(element.getType());
            if (element.getType().isArray()) {
                cast = "*(%s)".formatted(cast);
            }
            code += ("map[\"%s\"]?.let { %s(%s) }\n").formatted(name, name, cast);
        }

        code += "}\n return builder.build()";
        method.addCode(code);

        factory.addFunction(method.build());
    }

    private void annotationCodeBuilders(JavaAnnotationSource source) {
        var method = FunSpec.builder(methodCase(source.getName()) + "CodeGen")
                .receiver(KSAnnotation.class)
                .returns(ClassName.bestGuess("kotlin.String"));

        ClassName className = ClassName.bestGuess(builderName(source));
        fileBuilder.addImport(className.getPackageName(), className.getSimpleName());
        method.addCode(MessageFormat.format("""
                 val map = arguments
                            .map '{' it -> (it.name?.asString() ?: "value") to it.value }
                            .toMap()
                var code = "{0}Builder.{1}Builder()"
                """, source.getName(), methodCase(source.getName())));

        for (AnnotationElementSource element : source.getAnnotationElements()) {
            String name = element.getName();
            String value;

            method.addCode("""
                        map["%s"]?.let {
                    """.formatted(name));

            Type<JavaAnnotationSource> type = element.getType();
            if (type.getQualifiedName().startsWith("dev.morphia.annotations.")) {
                String typeName = type.getSimpleName();
                method.addCode("""
                        if (!Objects.equals(%sBuilder.defaults.%s, (it as KSAnnotation).to%s())) {
                        """.formatted(source.getName(), name, typeName));
                value = "${it.%sCodeGen()}".formatted(methodCase(typeName));
            } else {
                method.addCode("""
                        if (!Objects.equals(%sBuilder.defaults.%s, it)) {
                        """.formatted(source.getName(), name));

                value = getValue(type);
            }

            method.addCode("""
                        code += ".%s(%s)"
                      }
                    }
                    """.formatted(name, value));
        }

        method.addCode("""
                code += ".build()"
                """);
        method.addCode("return code");

        factory.addFunction(method.build());
    }

    private static String builderName(JavaSource<?> builder) {
        var pkg = builder.getPackage() + ".internal.";
        var name = builder.getName() + "Builder";
        return pkg + name;
    }

    private String getValue(Type<JavaAnnotationSource> type) {
        String typeName = type.getName();
        String code = "$it";

        if (typeName.equals("String")) {
            code = "\\\"%s\\\"".formatted(code);
        } else if (typeName.equals("Class")) {
            code += ".class";
        }
        return code;
    }

    private String processType(Type<JavaAnnotationSource> type) {
        String typeName = type.getName();
        String code = "NOT SET";
        String cast = "it as %s";

        if (typeName.equals("boolean")) {
            code = "Boolean";
        } else if (typeName.equals("String")) {
            code = "String";
        } else if (typeName.equals("int")) {
            code = "Int";
        } else if (typeName.equals("long")) {
            code = "Long";
            cast = "(it as Number).to%s()";
        } else if (typeName.equals("Class")) {
            code = "Class<*>";
        } else if (type.isArray()) {
            code = processArrayType(type);
        } else if (type.getQualifiedName().startsWith("com.mongodb.client.model.")
                || type.getQualifiedName().startsWith("dev.morphia.mapping.")) {
            code = type.getQualifiedName();
        } else if (type.getQualifiedName().startsWith("dev.morphia.annotations.")) {
            cast = "(it as KSAnnotation).to%s()";
            code = type.getSimpleName();
        } else {
            System.out.printf("unknown type: %n\t%s %n\t%s %n\t%s %n",
                    typeName,
                    type.getQualifiedName(),
                    type.getOrigin().isEnum());
            code = "<UNKNOWN>";
        }

        return cast.formatted(code);
    }

    @NotNull
    private static String processArrayType(Type<JavaAnnotationSource> type) {
        String code;
        code = type.getSimpleName();
        var parameterized = type.isParameterized();
        List<Type<JavaAnnotationSource>> params = parameterized ? type.getTypeArguments()
                : emptyList();
        if (!params.isEmpty()) {
            Type<JavaAnnotationSource> param = params.get(0);
            var parameterName = Types.toSimpleName(param.getQualifiedName());
            if (param.isWildcard()) {
                parameterName = parameterName.substring(parameterName.lastIndexOf(' ') + 1);
            }
            if (!Types.isQualified(parameterName)) {
                var target = parameterName;
                var imp = type.getOrigin().getImports().stream().filter(i -> i.getSimpleName().equals(target))
                        .findFirst().orElseThrow();
                parameterName = imp.getQualifiedName();
            }
            if (param.isWildcard()) {
                parameterName += "<*>";
            }

            code = "%s<%s>".formatted(type.getSimpleName(), parameterName);
        }
        code = "Array<out %s>".formatted(code);
        return code;
    }

    private Builder createFactory() {
        Builder annotationKspFactory = TypeSpec.objectBuilder("AnnotationKspFactory");
        var classBuilder = Roaster.create(JavaClassSource.class)
                .setName("AnnotationKspFactory")
                .setPackage(builders.values().iterator().next().getPackage() + ".internal")
                .setFinal(true);
        classBuilder.addAnnotation("dev.morphia.annotations.internal.MorphiaInternal");
        JavaDocSource<JavaClassSource> javaDoc = classBuilder.getJavaDoc();
        javaDoc.addTagValue("@since", "3.0");
        javaDoc.addTagValue("@hidden", "");
        javaDoc.addTagValue("@morphia.internal", "");

        return annotationKspFactory;
    }

    private List<File> find(String path) {
        File[] files = new File(path).listFiles(filter);
        return files != null ? asList(files) : List.of();
    }
}
