package dev.morphia.critter.parser;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.ClassfileOutput;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator;
import dev.morphia.critter.parser.gizmo.GizmoEntityModelGenerator;
import dev.morphia.critter.sources.Example;
import dev.morphia.mapping.ReflectiveMapper;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import io.github.classgraph.ClassGraph;

public class GeneratorTest {
    public static final CritterEntityModel entityModel;
    public static final CritterClassLoader critterClassLoader = new CritterClassLoader();

    static {
        ClassGraph classGraph = new ClassGraph()
                .addClassLoader(critterClassLoader)
                .enableAllInfo();
        classGraph.acceptPackages("dev.morphia.critter.sources");

        try (var scanResult = classGraph.scan()) {
            for (var classInfo : scanResult.getAllClasses()) {
                try {
                    ClassfileOutput.dump(critterClassLoader, classInfo.getName());
                } catch (Throwable ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        MorphiaConfig config = MorphiaConfig.load();
        Generators generators = new Generators(config, new ReflectiveMapper(config));
        GizmoEntityModelGenerator gen = CritterGizmoGenerator.generate(Example.class, critterClassLoader, generators, false);
        try {
            entityModel = (CritterEntityModel) critterClassLoader
                    .loadClass(gen.getGeneratedType())
                    .getConstructors()[0]
                    .newInstance(generators.getMapper());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object[][] methodNames(Class<?> clazz) {
        return methods(clazz).stream()
                .map(m -> new Object[] { m.getName(), m })
                .sorted(Comparator.comparing(a -> a[0].toString()))
                .toArray(Object[][]::new);
    }

    public static List<Method> methods(Class<?> clazz) {
        return Arrays.stream(clazz.getMethods())
                .filter(m -> !Modifier.isFinal(m.getModifiers()))
                .filter(m -> m.getParameterCount() == 0)
                .filter(m -> m.getDeclaringClass() == clazz)
                .collect(Collectors.toList());
    }

    /** Helper: remove list elements while predicate holds, then remove one more, return joined string. */
    static String removeWhile(List<String> list, Predicate<String> predicate) {
        List<String> removed = new ArrayList<>();
        while (!list.isEmpty() && predicate.test(list.get(0))) {
            removed.add(list.remove(0));
        }
        if (!list.isEmpty()) {
            removed.add(list.remove(0));
        }
        return String.join("\n", removed);
    }
}
