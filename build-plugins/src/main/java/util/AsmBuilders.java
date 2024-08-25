package util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jboss.forge.roaster.ParserException;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.AnnotationElementSource;
import org.jboss.forge.roaster.model.source.JavaAnnotationSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static util.AnnotationBuilders.methodCase;

@Mojo(name = "morphia-annotations-asm", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class AsmBuilders extends AbstractMojo {

    private Map<String, JavaAnnotationSource> builders = new TreeMap<>();

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private JavaClassSource factory;

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
            factory = createClass();
            factory.addImport(Type.class);
            factory.addImport(MethodVisitor.class);
            factory.addImport(AnnotationNode.class);

            var ifTree = new StringJoiner(" else ");
            builders.values().forEach(builder -> {
                String name = methodCase(builder.getName()) + "Type";
                factory.addField()
                        .setStatic(true)
                        .setFinal(true)
                        .setPrivate()
                        .setName(name)
                        .setType(String.class)
                        .setStringInitializer(descriptor(builder.getQualifiedName()));
                ifTree.add(MessageFormat.format("""
                        if (annotation.desc.equals({0})) '{'
                            emit{1}(mv, annotation);
                        '}'
                        """, name, builder.getName()));
            });

            var factoryMethod = factory.addMethod()
                    .setPublic()
                    .setStatic(true);
            factoryMethod
                    .setName("build")
                    .addParameter(MethodVisitor.class, "mv");
            factoryMethod
                    .addParameter(AnnotationNode.class, "annotation");

            var code = ifTree.toString();
            factoryMethod.setBody(code);

            emitBuilderConstruction(factory);
            invokeAnnotation(factory);
            invokeBoolean(factory);
            invokeClass(factory);
            invokeEnum(factory);
            invokeInt(factory);
            invokeLong(factory);
            invokeString(factory);
            for (JavaAnnotationSource source : builders.values()) {
                emitterMethod(factory, source);
            }
        }

        var outputFile = new File(generated, factory.getQualifiedName().replace('.', '/') + ".java");
        if (!outputFile.getParentFile().mkdirs() && !outputFile.getParentFile().exists()) {
            throw new IOException(format("Could not create directory: %s", outputFile.getParentFile()));
        }
        try (var writer = new FileWriter(outputFile)) {
            writer.write(factory.toString());
        }
    }

    private void emitBuilderConstruction(JavaClassSource factory) {
        var method = factory.addMethod()
                .setName("emitBuilderConstruction")
                .setStatic(true);
        method.addParameter(MethodVisitor.class, "mv");
        method.addParameter(Type.class, "builder");
        method.addParameter(String.class, "methodName");
        factory.addImport(Opcodes.class);

        var code = """
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/morphia/annotations/internal/PropertyBuilder",
                        "propertyBuilder", "()Ldev/morphia/annotations/internal/PropertyBuilder;", false);
                """;
        method.setBody(code);
    }

    private void emitterMethod(JavaClassSource factory, JavaAnnotationSource builder) throws ClassNotFoundException {
        var method = factory.addMethod()
                .setStatic(true)
                .setName("emit" + builder.getName());
        method.addParameter(MethodVisitor.class, "mv");
        method.addParameter(AnnotationNode.class, "node");

        var builderName = builderName(builder);

        Type builderType = Type.getType(descriptor(builderName));

        var code = MessageFormat.format("""
                emitBuilderConstruction(mv, Type.getType("{0}"), "{1}Builder");
                if (node.values != null) '{'
                    for (int i = 0; i < node.values.size(); i+=2) '{'
                        var name = (String)node.values.get(i);
                        var value = node.values.get(i++);""",
                builderType.getDescriptor(), methodCase(builder.getName()));

        var first = true;
        for (AnnotationElementSource element : builder.getAnnotationElements()) {
            if (!first) {
                code += " else ";
            }
            code += "if (name.equals(\"%s\")) {".formatted(element.getName());
            if (element.getType().getName().equals("String")) {
                code += "invokeString(mv, \"%s\", \"%s\", name, (String)value);".formatted(builderType.getDescriptor(),
                        builderType.getInternalName());
            } else if (element.getType().getName().equals("boolean")) {
                code += "invokeBoolean(mv, \"%s\", \"%s\", name, Boolean.valueOf((String)value));".formatted(builderType.getDescriptor(),
                        builderType.getInternalName());
            } else if (element.getType().getName().equals("int")) {
                code += "invokeInt(mv, \"%s\", \"%s\", name, (int)value);".formatted(builderType.getDescriptor(),
                        builderType.getInternalName());
            } else if (element.getType().getName().equals("long")) {
                code += "invokeLong(mv, \"%s\", \"%s\", name, (long)value);"
                        .formatted(builderType.getDescriptor(), builderType.getInternalName());
            } else if (element.getType().getName().equals("Class")) {
                code += "invokeClass(mv, \"%s\", \"%s\", name, (String)value);".formatted(builderType.getDescriptor(),
                        builderType.getInternalName());
            } else if (element.getType().getQualifiedName().startsWith("com.mongodb.client.model.")
                    || element.getType().getQualifiedName().startsWith("dev.morphia.mapping.")) {
                Type type = type(element.getType().getQualifiedName());
                code += "invokeEnum(mv, \"%s\", \"%s\", \"%s\", \"%s\", name, (String)value);"
                        .formatted(type.getDescriptor(), type.getInternalName(), builderType.getDescriptor(),
                                builderType.getInternalName());
            } else if (element.getType().isArray()) {
                System.out.printf("unknown type: %n\t%s %n\t%s %n\t%s %n",
                        element.getType().getName(),
                        element.getType().getQualifiedName(),
                        element.getType().getOrigin().isEnum());
            } else if (element.getType().getQualifiedName().startsWith("dev.morphia.annotations.")) {
                Type type = type(element.getType().getQualifiedName());
                code += "invokeAnnotation(mv, \"%s\", \"%s\", \"%s\", name, (AnnotationNode)value);"
                        .formatted(type.getDescriptor(), builderType.getDescriptor(),
                                builderType.getInternalName());
            } else {
                System.out.printf("unknown type: %n\t%s %n\t%s %n\t%s %n",
                        element.getType().getName(),
                        element.getType().getQualifiedName(),
                        element.getType().getOrigin().isEnum());

            }
            code += "}";
            first = false;
        }

        code += "}";
        code += "}";
        try {
            method.setBody(code);
        } catch (Exception e) {
            System.out.println("*****  code = " + code);
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private Type type(String qualifiedName) {
        return Type.getType(descriptor(qualifiedName));
    }

    private String descriptor(String qualifiedName) {
        return "L%s;".formatted(qualifiedName.replace('.', '/'));
    }

    private static String builderName(JavaSource<?> builder) {
        var pkg = builder.getPackage() + ".internal.";
        var name = builder.getName() + "Builder";
        return pkg + name;
    }

    private void invokeString(JavaClassSource factory) {
        var method = factory.addMethod()
                .setStatic(true)
                .setName("invokeString");
        method.addParameter(MethodVisitor.class, "mv");
        method.addParameter(String.class, "descriptor");
        method.addParameter(String.class, "internalName");
        method.addParameter(String.class, "method");
        method.addParameter(String.class, "value");

        method.setBody("""
                mv.visitLdcInsn(value);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, method,
                    "(Ljava/lang/String;)" + descriptor, false);""");
    }

    private void invokeBoolean(JavaClassSource factory) {
        var method = factory.addMethod()
                .setStatic(true)
                .setName("invokeBoolean");
        method.addParameter(MethodVisitor.class, "mv");
        method.addParameter(String.class, "descriptor");
        method.addParameter(String.class, "internalName");
        method.addParameter(String.class, "method");
        method.addParameter(boolean.class, "value");

        method.setBody("""
                mv.visitLdcInsn(value);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, method,
                    "(Z)" + descriptor, false);""");
    }

    private void invokeInt(JavaClassSource factory) {
        var method = factory.addMethod()
                .setStatic(true)
                .setName("invokeInt");
        method.addParameter(MethodVisitor.class, "mv");
        method.addParameter(String.class, "descriptor");
        method.addParameter(String.class, "internalName");
        method.addParameter(String.class, "method");
        method.addParameter(int.class, "value");

        method.setBody("""
                mv.visitLdcInsn(value);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, method,
                    "(I)" + descriptor, false);""");
    }

    private void invokeLong(JavaClassSource factory) {
        var method = factory.addMethod()
                .setStatic(true)
                .setName("invokeLong");
        method.addParameter(MethodVisitor.class, "mv");
        method.addParameter(String.class, "descriptor");
        method.addParameter(String.class, "internalName");
        method.addParameter(String.class, "method");
        method.addParameter(long.class, "value");

        method.setBody("""
                mv.visitLdcInsn(value);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, method,
                    "(J)" + descriptor, false);""");
    }

    private void invokeClass(JavaClassSource factory) {
        var method = factory.addMethod()
                .setStatic(true)
                .setName("invokeClass");
        method.addParameter(MethodVisitor.class, "mv");
        method.addParameter(String.class, "descriptor");
        method.addParameter(String.class, "internalName");
        method.addParameter(String.class, "method");
        method.addParameter(String.class, "value");

        method.setBody("""
                mv.visitLdcInsn(Type.getType(value));
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, method,
                    "(Ljava/lang/Class;)" + descriptor, false);""");
    }

    private void invokeEnum(JavaClassSource factory) {
        var method = factory.addMethod()
                .setStatic(true)
                .setName("invokeEnum");
        method.addParameter(MethodVisitor.class, "mv");
        method.addParameter(String.class, "enumDescriptor");
        method.addParameter(String.class, "enumInternalName");
        method.addParameter(String.class, "descriptor");
        method.addParameter(String.class, "internalName");
        method.addParameter(String.class, "method");
        method.addParameter(String.class, "value");

        method.setBody("""
                mv.visitFieldInsn(Opcodes.GETSTATIC, enumInternalName, value, enumDescriptor);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, method, "(" + enumDescriptor + ")" + descriptor, false);""");
    }

    private void invokeAnnotation(JavaClassSource factory) {
        var method = factory.addMethod()
                .setStatic(true)
                .setName("invokeAnnotation");
        method.addParameter(MethodVisitor.class, "mv");
        method.addParameter(String.class, "annotationDescriptor");
        method.addParameter(String.class, "descriptor");
        method.addParameter(String.class, "internalName");
        method.addParameter(String.class, "method");
        method.addParameter(AnnotationNode.class, "value");

        method.setBody("""
                build(mv, value);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, method, "(" + annotationDescriptor + ")" + descriptor, false);""");
    }

    /*
     * 
     * private fun AnnotationNode.toBuilder() {
     * var label = Label()
     * methodVisitor.visitLabel(label)
     * methodVisitor.visitLineNumber(24, label)
     * methodVisitor.visitVarInsn(ALOAD, 0)
     * val builderType = lookupBuilder(this)
     * methodVisitor.visitMethodInsn(
     * INVOKESTATIC,
     * builderType.internalName,
     * builderType.className.substringAfterLast(".").identifierCase(),
     * "()${builderType.descriptor}",
     * false
     * )
     * values.windowed(2, 2) { (name, value) ->
     * println("**************** name = ${name}")
     * println("**************** value = ${value}")
     * if (value is List<*>) {
     * "DUMMY"
     * } else {
     * methodVisitor.visitLdcInsn(value)
     * }
     * val label3 = Label()
     * methodVisitor.visitLabel(label3)
     * methodVisitor.visitLineNumber(25, label3)
     * val paramType = Type.getType(value::class.java)
     * methodVisitor.visitMethodInsn(
     * INVOKEVIRTUAL,
     * builderType.internalName,
     * name as String,
     * "(${paramType.descriptor})${builderType.descriptor}",
     * false
     * )
     * }
     * label = Label()
     * methodVisitor.visitLabel(label)
     * methodVisitor.visitLineNumber(26, label)
     * methodVisitor.visitMethodInsn(
     * INVOKEVIRTUAL,
     * builderType.internalName,
     * "build",
     * "()${this.desc}",
     * false
     * )
     * label = Label()
     * methodVisitor.visitLabel(label)
     * methodVisitor.visitLineNumber(24, label)
     * methodVisitor.visitMethodInsn(
     * INVOKEVIRTUAL,
     * generatedType.internalName,
     * "annotation",
     * "(Ljava/lang/annotation/Annotation;)Ldev/morphia/mapping/codec/pojo/PropertyModel;",
     * false
     * )
     * methodVisitor.visitInsn(POP)
     * }
     * 
     * 
     */
    private JavaClassSource createClass() {
        var classBuilder = Roaster.create(JavaClassSource.class)
                .setName("AnnotationAsmFactory")
                .setPackage(builders.values().iterator().next().getPackage() + ".internal")
                .setFinal(true);
        classBuilder.addAnnotation("dev.morphia.annotations.internal.MorphiaInternal");
        JavaDocSource<JavaClassSource> javaDoc = classBuilder.getJavaDoc();
        javaDoc.addTagValue("@since", "2.3");
        javaDoc.addTagValue("@hidden", "");
        javaDoc.addTagValue("@morphia.internal", "");

        return classBuilder;
    }

    private List<File> find(String path) {
        File[] files = new File(path).listFiles(filter);
        return files != null ? asList(files) : List.of();
    }
}
