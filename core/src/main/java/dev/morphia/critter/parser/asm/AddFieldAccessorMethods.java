package dev.morphia.critter.parser.asm;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import dev.morphia.critter.Critter;
import dev.morphia.critter.parser.FieldInfo;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassModel;
import io.github.dmlloyd.classfile.ClassTransform;
import io.github.dmlloyd.classfile.MethodModel;
import io.github.dmlloyd.classfile.TypeKind;

/**
 * Generates synthetic {@code __readXxx} and {@code __writeXxx} accessor methods directly
 * into an entity class bytecode for each of its fields.
 */
public class AddFieldAccessorMethods extends BaseGenerator {
    private final List<FieldInfo> fields;

    /**
     * Creates a generator that will add accessor methods for the given fields to the entity class.
     */
    public AddFieldAccessorMethods(Class<?> entity, List<FieldInfo> fields) {
        super(entity);
        this.fields = fields;
    }

    @Override
    public byte[] emit() {
        ClassModel model = readClassFiltering();
        ClassDesc entityDesc = ClassDesc.of(entity.getName());

        ClassTransform transform = ClassTransform.dropping(
                element -> element instanceof MethodModel m
                        && (m.flags().flagsMask() & ClassFile.ACC_SYNTHETIC) != 0
                        && (m.methodName().stringValue().startsWith("__read")
                                || m.methodName().stringValue().startsWith("__write")))
                .andThen(ClassTransform.endHandler(classBuilder -> {
                    for (FieldInfo field : fields) {
                        String name = field.name();
                        ClassDesc fieldDesc = ClassDesc.ofDescriptor(field.desc());
                        TypeKind kind = TypeKind.fromDescriptor(field.desc());

                        // __readXxx(): returns field type
                        String readerName = "__read%s".formatted(Critter.titleCase(name));
                        MethodTypeDesc readerMtd = MethodTypeDesc.of(fieldDesc);
                        classBuilder.withMethodBody(readerName, readerMtd,
                                ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC,
                                cod -> {
                                    cod.aload(0);
                                    cod.getfield(entityDesc, name, fieldDesc);
                                    cod.return_(kind);
                                });

                        // __writeXxx(fieldType): void
                        String writerName = "__write%s".formatted(Critter.titleCase(name));
                        MethodTypeDesc writerMtd = MethodTypeDesc.of(ClassDesc.ofDescriptor("V"), fieldDesc);
                        classBuilder.withMethodBody(writerName, writerMtd,
                                ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC,
                                cod -> {
                                    cod.aload(0);
                                    cod.loadLocal(kind, 1);
                                    cod.putfield(entityDesc, name, fieldDesc);
                                    cod.return_();
                                });
                    }
                }));

        return ClassFile.of().transformClass(model, transform);
    }
}
