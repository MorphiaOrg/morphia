package dev.morphia.rewrite.recipes.datastore;

import java.util.List;

import dev.morphia.Morphia;
import dev.morphia.config.MorphiaConfig;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaTemplate.Builder;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Class;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nullable;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;
import static dev.morphia.rewrite.recipes.RewriteUtils.methodMatcher;
import static org.openrewrite.java.JavaParser.fromJavaVersion;

public class CreateDatastoreMigrationVisitor extends JavaIsoVisitor<ExecutionContext> {
    private static final Logger LOG = LoggerFactory.getLogger(CreateDatastoreMigrationVisitor.class);
    private static final MethodMatcher CREATE_DATASTORE = methodMatcher("dev.morphia.Morphia", "createDatastore(..)");
    private static final String OLD_TYPE = "dev.morphia.mapping.MapperOptions";
    private static final String NEW_TYPE = "dev.morphia.config.MorphiaConfig";

    private static final String MORPHIA_CONFIG = ((Class) JavaType.buildType(MorphiaConfig.class.getName()))
            .getFullyQualifiedName();

    @Override
    public MethodInvocation visitMethodInvocation(@NotNull MethodInvocation methodInvocation, @NotNull ExecutionContext context) {
        if (CREATE_DATASTORE.matches(methodInvocation)) {
            List<Expression> arguments = methodInvocation.getArguments();
            if (arguments.size() == 1
                    || arguments.size() == 2 && arguments.get(1).getType() instanceof Class type
                            && type.getFullyQualifiedName().equals(MORPHIA_CONFIG)) {
                LOG.debug("Nothing to do on method match.  arguments:  {}%n", arguments);
                return methodInvocation;
            }

            maybeAddImport(NEW_TYPE, null, false);
            maybeAddImport("dev.morphia.Morphia", "createDatastore");
            maybeRemoveImport(OLD_TYPE);

            Expression options = arguments.size() == 3
                    ? convertToMorphiaConfig(arguments.get(1), arguments.get(2))
                    : synthesizeMorphiaConfig(arguments.get(1));
            maybeAddImport(NEW_TYPE);

            MethodInvocation newCreate = JavaTemplate.builder("createDatastore(#{any()}, #{any()})")
                    .javaParser(fromJavaVersion()
                            .classpath(findMorphiaDependencies()))
                    .imports(NEW_TYPE)
                    .staticImports(Morphia.class.getName() + ".createDatastore")
                    .build()
                    .apply(getCursor(), methodInvocation.getCoordinates().replace(), arguments.get(0), options);
            MethodInvocation updated = maybeAutoFormat(methodInvocation, newCreate, context);
            LOG.debug("builders matched: \n\t{}\nupdated to: \n\t{}", methodInvocation, updated);
            return updated;
        } else {
            return super.visitMethodInvocation(methodInvocation, context);
        }
    }

    public Expression synthesizeMorphiaConfig(Expression databaseName) {
        JavaTemplate databaseCall = JavaTemplate.builder("MorphiaConfig.load().database(#{any(java.lang.String)})")
                .javaParser(fromJavaVersion()
                        .classpath(findMorphiaDependencies()))
                .imports(NEW_TYPE)
                .build();

        return databaseCall.apply(new Cursor(getCursor(), databaseName), databaseName.getCoordinates().replace(), databaseName);
    }

    public Expression convertToMorphiaConfig(@Nullable Expression databaseName, Expression builder) {
        if (builder instanceof Identifier identifier) {
            return reuseArgument(getCursor(), identifier, databaseName);
        }
        JavaTemplate databaseCall = JavaTemplate.builder("MorphiaConfig.load().database(#{any(java.lang.String)})")
                .javaParser(fromJavaVersion()
                        .classpath(findMorphiaDependencies()))
                .imports(NEW_TYPE)
                .build();

        MethodInvocation applied = databaseCall.apply(new Cursor(getCursor(), builder), builder.getCoordinates().replace(), databaseName);

        return applied.withSelect(builder);
    }

    public static Expression reuseArgument(Cursor cursor, Identifier identifier, @Nullable Expression databaseName) {
        Builder dbBuilder = JavaTemplate.builder("MorphiaConfig.load().database(#{any()})");
        JavaTemplate databaseCall = dbBuilder
                .javaParser(fromJavaVersion()
                        .classpath(findMorphiaDependencies()))
                .imports(MORPHIA_CONFIG)
                .build();

        MethodInvocation apply = databaseCall.apply(new Cursor(cursor, identifier),
                identifier.getCoordinates().replace(), databaseName);
        apply = apply.withSelect(identifier);
        return apply;
    }
}
