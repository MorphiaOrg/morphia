package dev.morphia.query;

import dev.morphia.DeleteOptions;
import dev.morphia.FindAndModifyOptions;
import dev.morphia.InsertManyOptions;
import dev.morphia.InsertOneOptions;
import dev.morphia.InsertOptions;
import dev.morphia.TestBase;
import dev.morphia.UpdateOptions;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.Assert.fail;

public class OptionsTest extends TestBase {
    private final List<String> FILTER = List.of("clone");

    @Test
    public void validateReturnTypes() throws NoSuchFieldException {
        validate(CountOptions.class, "hintString(java.lang.String)");
        validate(FindAndDeleteOptions.class);
        validate(FindAndModifyOptions.class);
        validate(DeleteOptions.class);
        validate(InsertOptions.class);
        validate(UpdateOptions.class);

        validateComposed(InsertOneOptions.class);
        validateComposed(InsertManyOptions.class);
    }

    private void validateComposed(final Class<?> options, final String... localFilter) throws NoSuchFieldException {
        List<String> local = List.of(localFilter);
        Class<?> superclass = options.getDeclaredField("options").getType();
        Method[] declaredMethods = superclass.getDeclaredMethods();
        for (final Method method : declaredMethods) {
            if ((method.getReturnType().equals(superclass) || method.getName().startsWith("get")) && !FILTER.contains(method.getName())) {
                if (!local.contains(getShortName(method))) {
                    Method localMethod = null;
                    try {
                        localMethod = options.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException e) {
                        fail(format("%s does not implement the method %s", options.getName(), method));
                    }

                    if (!method.getName().startsWith("get")) {
                        Assert.assertEquals(format("%s should return its own type", localMethod), options, localMethod.getReturnType());
                    }
                }
            }
        }

    }

    private void validate(final Class<?> options, final String... localFilter) {
        List<String> local = List.of(localFilter);
        Class<?> superclass = options.getSuperclass();
        validate(options, local, superclass);
    }

    private void validate(final Class<?> options, final List<String> local, final Class<?> superclass) {
        Method[] declaredMethods = superclass.getDeclaredMethods();
        for (final Method method : declaredMethods) {
            if (method.getReturnType().equals(superclass) && !"clone".equals(method.getName())) {
                if (!local.contains(getShortName(method))) {
                    Method localMethod = null;
                    try {
                        localMethod = options.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException e) {
                        fail(format("%s does not implement the method %s", options.getName(), method));
                    }

                    Assert.assertEquals(format("%s should return its own type", localMethod), options, localMethod.getReturnType());
                }
            }
        }
    }

    private String getShortName(final Method method) {
        List<String> parameterTypes = List.of(method.getParameterTypes())
            .stream().map(c -> c.getName())
            .collect(Collectors.toList());

        String name = format("%s(%s)", method.getName(), String.join(", ", parameterTypes));
        return name;
    }
}