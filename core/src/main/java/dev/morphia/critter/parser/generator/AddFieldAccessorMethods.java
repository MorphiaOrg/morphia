package dev.morphia.critter.parser.generator;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import dev.morphia.critter.Critter;
import dev.morphia.critter.parser.FieldInfo;

import io.smallrye.classfile.ClassBuilder;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.ClassTransform;
import io.smallrye.classfile.FieldModel;
import io.smallrye.classfile.Label;
import io.smallrye.classfile.MethodModel;
import io.smallrye.classfile.TypeKind;

/**
 * Generates synthetic {@code __readXxx} and {@code __writeXxx} accessor methods directly
 * into an entity class bytecode for each of its fields.
 */
public class AddFieldAccessorMethods extends AccessorMethods {
    private static final ClassDesc CD_FIELD = ClassDesc.of("java.lang.reflect.Field");
    private static final ClassDesc CD_CRITTER = ClassDesc.of("dev.morphia.critter.Critter");
    private static final ClassDesc CD_RUNTIME_EXCEPTION = ClassDesc.of("java.lang.RuntimeException");
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
                                || m.methodName().stringValue().startsWith("__write"))
                        || element instanceof FieldModel f
                                && (f.flags().flagsMask() & ClassFile.ACC_SYNTHETIC) != 0
                                && f.fieldName().stringValue().startsWith("__field"))
                .andThen(ClassTransform.endHandler(classBuilder -> {
                    for (FieldInfo field : fields) {
                        String name = field.name();
                        ClassDesc fieldDesc = ClassDesc.ofDescriptor(field.desc());
                        TypeKind kind = TypeKind.fromDescriptor(field.desc());

                        // __readXxx(): returns Object (widens from concrete type inside entity,
                        // keeping non-public types out of the accessor's constant pool)
                        String readerName = "__read%s".formatted(Critter.titleCase(name));
                        boolean isPrimitive = kind != TypeKind.REFERENCE;
                        MethodTypeDesc readerMtd = MethodTypeDesc.of(isPrimitive ? fieldDesc : ConstantDescs.CD_Object);
                        classBuilder.withMethodBody(readerName, readerMtd,
                                ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC,
                                cod -> {
                                    cod.aload(0);
                                    cod.getfield(entityDesc, name, fieldDesc);
                                    cod.return_(kind);
                                });

                        // __writeXxx(Object): void  (cast to concrete type inside entity where it's accessible)
                        String writerName = "__write%s".formatted(Critter.titleCase(name));
                        MethodTypeDesc writerMtd = MethodTypeDesc.of(ClassDesc.ofDescriptor("V"),
                                isPrimitive ? fieldDesc : ConstantDescs.CD_Object);
                        if ((field.access() & ClassFile.ACC_FINAL) != 0) {
                            writeFinalField(classBuilder, entityDesc, field, writerName, writerMtd, kind, fieldDesc);
                            continue;
                        }
                        classBuilder.withMethodBody(writerName, writerMtd,
                                ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC,
                                cod -> {
                                    cod.aload(0);
                                    if (isPrimitive) {
                                        cod.loadLocal(kind, 1);
                                    } else {
                                        cod.aload(1);
                                        cod.checkcast(fieldDesc);
                                    }
                                    cod.putfield(entityDesc, name, fieldDesc);
                                    cod.return_();
                                });
                    }
                }));

        return ClassFile.of().transformClass(model, transform);
    }

    /**
     * {@code putfield} may only write a final field from a constructor, so the writer goes through reflection instead.
     * The {@link java.lang.reflect.Field} is looked up on its declaring class on first use and cached in a synthetic
     * volatile static field, so other threads see it fully initialized.
     */
    private static void writeFinalField(ClassBuilder classBuilder, ClassDesc entityDesc,
            FieldInfo field, String writerName, MethodTypeDesc writerMtd, TypeKind kind, ClassDesc fieldDesc) {
        String cacheName = "__field%s".formatted(Critter.titleCase(field.name()));
        classBuilder.withField(cacheName, CD_FIELD,
                ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_VOLATILE | ClassFile.ACC_SYNTHETIC);
        classBuilder.withMethodBody(writerName, writerMtd, ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC, cod -> {
            Label cached = cod.newLabel();
            cod.getstatic(entityDesc, cacheName, CD_FIELD);
            cod.dup();
            cod.ifnonnull(cached);
            cod.pop();
            cod.ldc(entityDesc);
            if (field.declaringClass() != null) {
                cod.ldc(field.declaringClass().getName());
                cod.ldc(field.name());
                cod.invokestatic(CD_CRITTER, "accessibleField",
                        MethodTypeDesc.of(CD_FIELD, ConstantDescs.CD_Class, ConstantDescs.CD_String, ConstantDescs.CD_String));
            } else {
                cod.ldc(field.name());
                cod.invokestatic(CD_CRITTER, "accessibleField",
                        MethodTypeDesc.of(CD_FIELD, ConstantDescs.CD_Class, ConstantDescs.CD_String));
            }
            cod.dup();
            cod.putstatic(entityDesc, cacheName, CD_FIELD);
            cod.labelBinding(cached);
            int fieldSlot = cod.allocateLocal(TypeKind.REFERENCE);
            cod.astore(fieldSlot);
            // Wrap failures the same way the runtime accessor (NestmateAccessorGenerator) does, so both tiers fail alike
            // and Field.set's checked IllegalAccessException doesn't escape undeclared.
            cod.trying(tryBody -> {
                tryBody.aload(fieldSlot);
                tryBody.aload(0);
                tryBody.loadLocal(kind, 1);
                if (kind != TypeKind.REFERENCE) {
                    ClassDesc wrapper = ClassDesc.of(GenerationUtils.PRIMITIVE_TO_WRAPPER.get(fieldDesc.displayName()));
                    tryBody.invokestatic(wrapper, "valueOf", MethodTypeDesc.of(wrapper, fieldDesc));
                }
                tryBody.invokevirtual(CD_FIELD, "set", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object,
                        ConstantDescs.CD_Object));
                tryBody.return_();
            }, catches -> catches.catching(ConstantDescs.CD_Exception, catchBody -> {
                int exceptionSlot = catchBody.allocateLocal(TypeKind.REFERENCE);
                catchBody.astore(exceptionSlot);
                catchBody.new_(CD_RUNTIME_EXCEPTION);
                catchBody.dup();
                catchBody.ldc("Failed to set final field '%s'".formatted(field.name()));
                catchBody.aload(exceptionSlot);
                catchBody.invokespecial(CD_RUNTIME_EXCEPTION, "<init>",
                        MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String, ConstantDescs.CD_Throwable));
                catchBody.athrow();
            }));
        });
    }
}
