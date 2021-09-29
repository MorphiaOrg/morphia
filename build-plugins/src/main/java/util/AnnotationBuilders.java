package util;

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

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Mojo(name = "morphia-annotations", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class AnnotationBuilders extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

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
        files.addAll(find(project.getBasedir() + "/src/main/java/dev/morphia/annotations/experimental"));
        project.addCompileSourceRoot(generated.getAbsolutePath());

        for (File file : files) {
            try {
                emitBuilder(file);
            } catch (ParserException e) {
                throw new MojoExecutionException("Could not parse " + file, e);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
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
        builder = Roaster.create(JavaClassSource.class)
                         .setName(source.getName() + "Builder")
                         .setPackage(source.getPackage() + ".builders")
                         .setFinal(true);
        JavaDocSource<JavaClassSource> javaDoc = builder.getJavaDoc();
        javaDoc.addTagValue("@since", "2.3");
        javaDoc.addTagValue("@morphia.internal", "");
        javaDoc.addTagValue("@morphia.experimental", "");

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

            builder.addMethod()
                   .setPublic()
                   .setName(name)
                   .setReturnType(builder.getName())
                   .setBody(format("annotation.%s = %s; return this;", name, name))
                   .addParameter(parameterType(element), name);
        }

        for (Import anImport : source.getImports()) {
            builder.addImport(anImport);
        }
        builder.addImport(Objects.class);
        output();
    }

    private String parameterType(AnnotationElementSource element) {
        return element.getType().toString().replace("[]", "...");
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
                                             + "/builders/" + builder.getName() + ".java");
        outputFile.getParentFile().mkdirs();
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
                    literal = format("%sBuilder.%s().build()", annot.getName(), builderMethodName(annot.getName()));
                } else if (literal != null && element.getType().isArray()) {
                    literal = format("new %s%s", element.getType().getName(), literal);
                }
                if (literal != null) {
                    body += format("annotation.%s = %s;\n", element.getName(), literal);
                }
            }
        }
        constructor.setBody(body);

    }
}
