package dev.morphia.critter.parser.generator;

import java.lang.constant.ConstantDescs;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassTransform;
import io.smallrye.classfile.FieldModel;

/**
 * Stamps a class woven at build time with a synthetic static field named for a hash of its woven bytecode. A generated
 * model reads the stamps of the classes it calls into when it's initialized, so a model whose classes were compiled again
 * without being woven, or woven differently, fails to load rather than failing the first time an entity is encoded.
 */
final class WeaveStamp {
    private static final String PREFIX = "__woven$";

    private WeaveStamp() {
    }

    /**
     * @return a transform dropping the stamp left by an earlier build, so weaving a woven class again stamps it afresh
     */
    static ClassTransform dropping() {
        return ClassTransform.dropping(element -> element instanceof FieldModel f && isStamp(f));
    }

    /**
     * @param woven a woven class's bytecode, without a stamp
     * @return the bytecode with its stamp
     */
    static byte[] stamp(byte[] woven) {
        String name = PREFIX + hash(woven);
        return ClassFile.of().transformClass(ClassFile.of().parse(woven), ClassTransform.endHandler(cb -> cb.withField(name,
                ConstantDescs.CD_boolean, ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL
                        | ClassFile.ACC_SYNTHETIC)));
    }

    /**
     * @param bytes a class's bytecode
     * @return the name of the class's stamp, or null if it wasn't woven
     */
    static String of(byte[] bytes) {
        return ClassFile.of().parse(bytes).fields().stream()
                .filter(WeaveStamp::isStamp)
                .map(f -> f.fieldName().stringValue())
                .findFirst()
                .orElse(null);
    }

    private static boolean isStamp(FieldModel field) {
        return (field.flags().flagsMask() & ClassFile.ACC_SYNTHETIC) != 0 && field.fieldName().stringValue().startsWith(PREFIX);
    }

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(bytes), 8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
