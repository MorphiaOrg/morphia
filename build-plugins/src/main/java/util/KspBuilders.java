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
import org.jboss.forge.roaster.model.source.AnnotationElementSource;
import org.jboss.forge.roaster.model.source.JavaAnnotationSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static util.AnnotationBuilders.methodCase;

@Mojo(name = "morphia-annotations-ksp", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class KspBuilders extends AbstractMojo {

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
        generated = new File(project.getBasedir() + "/target/generated-sources/morphia-annotations-ksp/");

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

            /*
             * var ifTree = new StringJoiner(" else ");
             * builders.values().forEach(builder -> {
             * String name = methodCase(builder.getName()) + "Type";
             * factory.addProperty()
             * .setStatic(true)
             * .setFinal(true)
             * .setPrivate()
             * .setName(name)
             * .setType(String.class)
             * .setStringInitializer(descriptor(builder.getQualifiedName()));
             * ifTree.add(MessageFormat.format("""
             * if (annotation.desc.equals({0})) '{'
             * emit{1}(mv, annotation);
             * '}'
             * """, name, builder.getName()));
             * });
             */
            /*
             * var factoryMethod = factory.addMethod()
             * .setPublic()
             * .setStatic(true);
             * factoryMethod
             * .setName("build")
             * .setReturnType("A")
             * .addTypeVariable()
             * .setName("A")
             * .setBounds(Annotation.class);
             * factoryMethod
             * .addParameter(KSAnnotation.class, "annotation");
             *
             * factoryMethod.setBody(ifTree.toString());
             */

            //            emitBuilderConstruction(factory);
            //            invokeAnnotation(factory);
            //            invokeBoolean(factory);
            //            invokeClass(factory);
            //            invokeEnum(factory);
            //            invokeInt(factory);
            //            invokeLong(factory);
            //            invokeString(factory);
            for (JavaAnnotationSource source : builders.values()) {
                annotationMethod(factory, source);
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

    private void annotationMethod(Builder factory, JavaAnnotationSource source) throws ClassNotFoundException {
        var method = FunSpec.builder("to" + source.getName())
                .receiver(KSAnnotation.class)
                .returns(ClassName.bestGuess(source.getQualifiedName()));
        //        method.addParameter(KSAnnotation.class, "annotation");

        var builderName = builderName(source);

        Type builderType = Type.getType(descriptor(builderName));

        var code = MessageFormat.format("""
                val map = arguments
                    .map '{' it -> (it.name?.asString() ?: "value") to it.value }
                    .toMap()
                var builder = {0}Builder.{1}Builder().apply '{'
                """,
                source.getName(), methodCase(source.getName()));

        for (AnnotationElementSource element : source.getAnnotationElements()) {
            String typeName = element.getType().getName();
            code = processType(element, typeName, code);
        }

        code += "}\n return builder.build()";
        try {
            method.addCode(code);
        } catch (Exception e) {
            System.out.println("*****  code = " + code);
            System.out.println(e.getMessage());
            System.exit(1);
        }

        fileBuilder.addFunction(method.build());
    }

    private String processType(AnnotationElementSource element, String typeName, String code) {
        var name = element.getName();
        var type = element.getType();
        if (typeName.equals("boolean")) {
            code += "map[\"%s\"]?.let { %s(it as Boolean) }\n".formatted(name, name);
        } else if (typeName.equals("String")) {
            code += "map[\"%s\"]?.let { %s(it as String) }\n".formatted(name, name);
        } else if (typeName.equals("int")) {
            code += "map[\"%s\"]?.let { %s(it as Int) }\n".formatted(name, name);
        } else if (typeName.equals("long")) {
            code += "map[\"%s\"]?.let { %s(it as Long) }\n".formatted(name, name);
        } else if (typeName.equals("Class")) {
            code += "map[\"%s\"]?.let { %s(it as Class<*>) }\n".formatted(name, name);
        } else if (type.getQualifiedName().startsWith("com.mongodb.client.model.")
                || type.getQualifiedName().startsWith("dev.morphia.mapping.")) {
            code += "map[\"%s\"]?.let { %s(it as %s) }\n".formatted(name, name, type.getQualifiedName());
        } else if (type.getQualifiedName().startsWith("dev.morphia.annotations.")) {
            var array = type.isArray();
            var splat = "";
            if (array) {
                splat = "*";
            }
            code += "map[\"%s\"]?.let { %s(%s(it as KSAnnotation)) }\n"
                    .formatted(name, name, typeName, splat);
        } else if (type.isArray()) {
            var parameterized = type.isParameterized();
            List<org.jboss.forge.roaster.model.Type<JavaAnnotationSource>> params = parameterized ? type.getTypeArguments()
                    : emptyList();
            String simpleName = type.getSimpleName();
            if (!params.isEmpty()) {
                var paramSimpleName = params.get(0).getName().replace('?', '*');
                int beginIndex = paramSimpleName.indexOf(("extends")) + 8;
                simpleName = "%s<%s>".formatted(type.getSimpleName(), paramSimpleName.substring(beginIndex));
            }
            code += "map[\"%s\"]?.let { %s(*(it as Array<out %s>)) }\n"
                    .formatted(name, name, simpleName, typeName);
        } else {
            System.out.printf("unknown type: %n\t%s %n\t%s %n\t%s %n",
                    typeName,
                    type.getQualifiedName(),
                    type.getOrigin().isEnum());

        }
        return code;
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
