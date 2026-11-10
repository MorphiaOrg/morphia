package dev.morphia.critter.parser.generator;

import java.lang.constant.ConstantDescs;
import java.security.SecureRandom;
import java.util.HexFormat;

import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassTransform;
import io.smallrye.classfile.FieldModel;

/**
 * Stamps a class woven at build time with the ID of the critter-maven run that wove it: a private synthetic static field
 * named {@link CritterEntityModel#BUILD_STAMP} plus the ID. The models generated in the same run record the ID, so the
 * mapper can tell when a class they use was compiled again, or woven by another run, after they were generated. Being
 * private and static, the field leaves a {@code Serializable} class's default {@code serialVersionUID} alone.
 */
final class BuildStamp {
    private static final SecureRandom RANDOM = new SecureRandom();

    private BuildStamp() {
    }

    /**
     * @return a new build ID
     */
    static String newId() {
        byte[] id = new byte[8];
        RANDOM.nextBytes(id);
        return HexFormat.of().formatHex(id);
    }

    /**
     * @return a transform dropping the stamp left by an earlier build
     */
    static ClassTransform dropping() {
        return ClassTransform.dropping(element -> element instanceof FieldModel f && isStamp(f));
    }

    /**
     * @param woven   a woven class's bytecode, without a stamp
     * @param buildId the build's ID
     * @return the bytecode with its stamp
     */
    static byte[] stamp(byte[] woven, String buildId) {
        return ClassFile.of().transformClass(ClassFile.of().parse(woven), ClassTransform.endHandler(cb -> cb.withField(
                CritterEntityModel.BUILD_STAMP + buildId, ConstantDescs.CD_boolean,
                ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC)));
    }

    /**
     * @param bytes a class's bytecode
     * @return true if the class was woven
     */
    static boolean isStamped(byte[] bytes) {
        return ClassFile.of().parse(bytes).fields().stream().anyMatch(BuildStamp::isStamp);
    }

    private static boolean isStamp(FieldModel field) {
        return (field.flags().flagsMask() & ClassFile.ACC_SYNTHETIC) != 0
                && field.fieldName().stringValue().startsWith(CritterEntityModel.BUILD_STAMP);
    }
}
