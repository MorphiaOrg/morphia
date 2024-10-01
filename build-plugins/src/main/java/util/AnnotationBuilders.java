package util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jboss.forge.roaster.ParserException;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.AnnotationElementSource;
import org.jboss.forge.roaster.model.source.AnnotationElementSource.DefaultValue;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaAnnotationSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Mojo(name = "morphia-annotations", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class AnnotationBuilders extends AbstractMojo {

    private Map<String, JavaClassSource> builders = new TreeMap<>();

    private MethodSource<JavaClassSource> factoryMethod;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private JavaClassSource factory;

    private JavaClassSource builder;

    private JavaAnnotationSource source;

    private File generated;

    private final FileFilter filter = pathname -> pathname.getName().endsWith(".java")
            && !pathname.getName().endsWith("Handler.java")
            && !pathname.getName().endsWith("Helper.java")
            && !pathname.getName().equals("package-info.java");

    @Override
    @SuppressWarnings("ConstantConditions")
    public void execute() throws MojoExecutionException {
        List<File> files = new ArrayList<>();
        generated = new File(project.getBasedir() + "/target/generated-sources/morphia-annotations/");

        files.addAll(find(project.getBasedir() + "/src/main/java/dev/morphia/annotations"));
        project.addCompileSourceRoot(generated.getAbsolutePath());

        try {
            for (File file : files) {
                try {
                    emitBuilder(file);
                } catch (ParserException e) {
                    throw new MojoExecutionException("Could not parse " + file, e);
                }
            }
            emitFactory();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void emitFactory() throws IOException {
        if (factory == null) {
            factory = createClass("AnnotationFactory");

            factoryMethod = factory.addMethod()
                    .setReturnType("K")
                    .setPublic()
                    .setStatic(true);
            factoryMethod
                    .setName("build")
                    .addParameter("Class<K>", "kind");

            factoryMethod.addTypeVariable()
                    .setName("K");
            var code = builders.entrySet().stream()
                    .map(entry -> {
                        JavaClassSource builder = entry.getValue();
                        return "if (kind.equals(%s.class)) return (K)%s.%s().build();".formatted(entry.getKey(),
                                builder.getQualifiedName(),
                                methodCase(builder.getName()));
                    })
                    .collect(Collectors.joining("\n"));
            code += "\nthrow new UnsupportedOperationException(kind.getName());";
            factoryMethod.setBody(code);
        }

        var outputFile = new File(generated, factory.getQualifiedName().replace('.', '/') + ".java");
        if (!outputFile.getParentFile().mkdirs() && !outputFile.getParentFile().exists()) {
            throw new IOException(format("Could not create directory: %s", outputFile.getParentFile()));
        }
        try (var writer = new FileWriter(outputFile)) {
            writer.write(factory.toString());
        }

    }

    private JavaClassSource annotationType(JavaAnnotationSource source, JavaClassSource builder) {
        JavaClassSource morphiaAnnotation = builder.addNestedType(JavaClassSource.class)
                .addInterface(source.getQualifiedName())
                .setName(format("%sAnnotation", source.getName()))
                .setStatic(true)
                .setPrivate();

        morphiaAnnotation.addMethod()
                .setPublic()
                .setName("annotationType")
                .setReturnType(format("Class<%s>", source.getName()))
                .setBody(format("return %s.class;", source.getName()));

        builder.addField()
                .setPrivate()
                .setName("annotation")
                .setType(format("%sAnnotation", source.getName()))
                .setLiteralInitializer(format("new %sAnnotation()", source.getName()));

        return morphiaAnnotation;
    }

    private String builderMethodName(String input) {
        return input.substring(0, 1).toLowerCase(Locale.getDefault()) + input.substring(1) + "Builder";
    }

    private void copyBuilder(JavaAnnotationSource source, List<AnnotationElementSource> elements) {
        MethodSource<JavaClassSource> copier = builder.addMethod()
                .setPublic()
                .setStatic(true)
                .setName(builderMethodName(source.getName()))
                .setReturnType(builder.getName())
                .setBody(format("return new %s();", builder.getName()));
        copier.addParameter(source.getName(), "source");
        StringJoiner copies = new StringJoiner("\n");
        copies.add(format("var builder =  new %s();", builder.getName()));

        for (AnnotationElementSource element : elements) {
            String name = element.getName();
            copies.add(format("builder.annotation.%s = source.%s();", name, name));
        }

        copies.add("return builder;");
        copier.setBody(copies.toString());
    }

    private void emitBuilder(File sourceFile) throws IOException {
        source = Roaster.parse(JavaAnnotationSource.class, sourceFile);
        if (source.isPublic()) {
            builder = createClass(source.getName() + "Builder");
            builders.put(source.getQualifiedName(), builder);

            MethodSource<JavaClassSource> constructor = builder.addMethod()
                    .setConstructor(true)
                    .setPrivate();
            setDefaults(constructor, source);

            builder.addMethod()
                    .setPublic()
                    .setName("build")
                    .setReturnType(source.getName())
                    .setBody(format("var anno = annotation; annotation = null; return anno;", builder.getName()));

            builder.addMethod()
                    .setPublic()
                    .setStatic(true)
                    .setName(builderMethodName(source.getName()))
                    .setReturnType(builder.getName())
                    .setBody(format("return new %s();", builder.getName()));

            builder.addField()
                    .setName("defaults")
                    .setStatic(true)
                    .setPublic()
                    .setType(source.getName())
                    .setLiteralInitializer("%s().build()".formatted(builderMethodName(source.getName())));

            JavaClassSource morphiaAnnotation = annotationType(source, builder);
            List<AnnotationElementSource> elements = source.getAnnotationElements();
            if (!elements.isEmpty()) {
                copyBuilder(source, elements);
                equals(morphiaAnnotation, elements);
                hashCode(morphiaAnnotation, elements);
            }
            for (AnnotationElementSource element : elements) {
                String name = element.getName();
                morphiaAnnotation.addField()
                        .setName(name)
                        .setType(element.getType().toString())
                        .setPrivate();

                morphiaAnnotation.addMethod()
                        .setPublic()
                        .setName(name)
                        .setReturnType(element.getType())
                        .setBody(format("return %s;", name))
                        .addAnnotation(Override.class);

                String parameterType = parameterType(element);
                boolean varargs = false;
                if (parameterType.endsWith("[]")) {
                    parameterType = parameterType.substring(0, parameterType.length() - 2);
                    varargs = true;
                }
                builder.addMethod()
                        .setPublic()
                        .setName(name)
                        .setReturnType(builder.getName())
                        .setBody(
                                format("annotation.%s = %s; return this;", name,
                                        name))
                        .addParameter(parameterType, name).setVarArgs(varargs);

                if (element.getType().isPrimitive()) {
                    builder.addMethod()
                            .setPublic()
                            .setName(name)
                            .setReturnType(builder.getName())
                            .setBody(
                                    format("annotation.%s = %s; return this;", name,
                                            name))
                            .addParameter(wrapper(parameterType), name).setVarArgs(varargs);
                }

            }

            for (Import anImport : source.getImports()) {
                builder.addImport(anImport);
            }
            builder.addImport(Objects.class);
            output();
        }
    }

    private String wrapper(String type) {
        return switch (type) {
            case "int" -> "Integer";
            case "char" -> "Character";
            default -> type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1);
        };
    }

    private JavaClassSource createClass(String name) {
        var classBuilder = Roaster.create(JavaClassSource.class)
                .setName(name)
                .setPackage(source.getPackage() + ".internal")
                .setFinal(true);
        classBuilder.addAnnotation("dev.morphia.annotations.internal.MorphiaInternal");
        JavaDocSource<JavaClassSource> javaDoc = classBuilder.getJavaDoc();
        javaDoc.addTagValue("@since", "2.3");
        javaDoc.addTagValue("@hidden", "");
        javaDoc.addTagValue("@morphia.internal", "");

        return classBuilder;
    }

    private String parameterType(AnnotationElementSource element) {
        return element.getType().toString();
    }

    private void equals(JavaClassSource annotation, List<AnnotationElementSource> elements) {
        StringJoiner comparisons = new StringJoiner(" && ", "return ", ";");
        for (AnnotationElementSource element : elements) {
            String comparator;
            if (element.getType().isArray()) {
                builder.addImport(Arrays.class);

                comparator = "Arrays";
            } else {
                comparator = "Objects";
            }
            comparisons.add(format("%s.equals(%s, that.%s)", comparator, element.getName(),
                    element.getName()));
        }
        annotation.addMethod()
                .setName("equals")
                .setReturnType(boolean.class)
                .setPublic()
                .setBody("if (this == o) {\n"
                        + "   return true;\n"
                        + "}\n"
                        + "if (!(o instanceof " + annotation.getName() + ")) {\n"
                        + "   return false;\n"
                        + "}\n"
                        + "var that = (" + annotation.getName() + ") o;\n"
                        + comparisons)
                .addParameter("Object", "o");

    }

    private List<File> find(String path) {
        File[] files = new File(path).listFiles(filter);
        return files != null ? asList(files) : List.of();
    }

    private void hashCode(JavaClassSource annotation, List<AnnotationElementSource> elements) {
        StringJoiner values = new StringJoiner(", ", "return Objects.hash(", ");");
        for (AnnotationElementSource element : elements) {
            values.add(element.getName());
        }
        annotation.addMethod()
                .setName("hashCode")
                .setReturnType(int.class)
                .setPublic()
                .setBody(values.toString());
    }

    private void output() throws IOException {
        var outputFile = new File(generated, source.getPackage().replace('.', '/')
                + "/internal/" + builder.getName() + ".java");
        if (!outputFile.getParentFile().mkdirs() && !outputFile.getParentFile().exists()) {
            throw new IOException(format("Could not create directory: %s", outputFile.getParentFile()));
        }
        try (var writer = new FileWriter(outputFile)) {
            writer.write(builder.toString());
        }
    }

    private void setDefaults(MethodSource<JavaClassSource> constructor, JavaAnnotationSource source) {
        String body = "";
        List<AnnotationElementSource> elements = source.getAnnotationElements();
        for (AnnotationElementSource element : elements) {
            DefaultValue defaultValue = element.getDefaultValue();
            if (defaultValue != null) {
                String literal = defaultValue.getLiteral();
                var annot = defaultValue.getAnnotation();
                if (annot != null) {
                    literal = format("%sBuilder.%s().build()",
                            annot.getQualifiedName().replace(annot.getName(), "") + "internal." + annot.getName(),
                            builderMethodName(annot.getName()));
                } else if (literal != null && element.getType().isArray()) {
                    literal = format("new %s%s", element.getType().getName(), literal);
                }
                if (literal != null) {
                    body += format("annotation.%s = %s;%n", element.getName(), literal);
                }
            }
        }
        constructor.setBody(body);

    }

    public static String methodCase(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

}
