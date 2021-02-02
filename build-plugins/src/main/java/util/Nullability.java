package util;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;
import com.puppycrawl.tools.checkstyle.utils.TokenUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Set;

import static com.puppycrawl.tools.checkstyle.api.TokenTypes.CTOR_DEF;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.IDENT;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_BOOLEAN;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_BYTE;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_CHAR;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_DOUBLE;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_FLOAT;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_INT;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_LONG;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_SHORT;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_VOID;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.METHOD_DEF;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.MODIFIERS;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.PARAMETERS;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.PARAMETER_DEF;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.TYPE;
import static com.puppycrawl.tools.checkstyle.utils.AnnotationUtil.containsAnnotation;

@SuppressWarnings({"unused", "Nullability"})
public class Nullability extends AbstractCheck {
    public static final String METHOD_KEY = "method.annotation";
    public static final String PARAM_KEY = "parameter.annotation";
    private static final List<String> NAMES = List.of("NonNull", "Nullable");
    private final Set<Integer> primitives = Set.of(
        LITERAL_BYTE,
        LITERAL_SHORT,
        LITERAL_INT,
        LITERAL_LONG,
        LITERAL_FLOAT,
        LITERAL_DOUBLE,
        LITERAL_VOID,
        LITERAL_BOOLEAN,
        LITERAL_CHAR);

    @Override
    public int[] getDefaultTokens() {
        return new int[]{
            METHOD_DEF,
            CTOR_DEF,
            PARAMETER_DEF
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[]{
            METHOD_DEF,
            CTOR_DEF,
            PARAMETER_DEF
        };
    }

    @Override
    public int[] getRequiredTokens() {
        return CommonUtil.EMPTY_INT_ARRAY;
    }

    @Override
    public void visitToken(@NonNull DetailAST ast) {
        final DetailAST parameters = ast.findFirstToken(PARAMETERS);
        if (isNonPrivate(ast)) {
            checkReturnType(ast);
            if (parameters != null) {
                TokenUtil.forEachChild(parameters, PARAMETER_DEF, this::checkParam);
            }
        }
    }

    private void checkParam(DetailAST param) {
        if (!primitive(param)) {
            if (!containsAnnotation(param, NAMES)) {
                log(param, PARAM_KEY, param.findFirstToken(IDENT).getText());
            }
        }
    }

    private void checkReturnType(DetailAST method) {
        if (method.getType() == CTOR_DEF) {
            return;
        }
        String methodName = method.findFirstToken(IDENT).getText();
        DetailAST returnType = method.getFirstChild().getNextSibling();
        boolean primitive = primitives.contains(returnType.getFirstChild().getType());
        boolean containsAnnotation = containsAnnotation(method, NAMES);
        if (!primitive && !containsAnnotation) {
            log(method, METHOD_KEY, methodName);
        }
    }

    private DetailAST findMethod(DetailAST ast) {
        DetailAST parent = ast.getParent();
        while (parent.getType() != CTOR_DEF && parent.getType() != METHOD_DEF) {
            parent = parent.getParent();
        }
        return parent;
    }

    private boolean isNonPrivate(DetailAST method) {
        DetailAST modifiers = method.findFirstToken(MODIFIERS);
        return modifiers.findFirstToken(TokenTypes.LITERAL_PUBLIC) != null
               || modifiers.findFirstToken(TokenTypes.LITERAL_PROTECTED) != null;
    }

    private boolean primitive(DetailAST paramDef) {
        return primitives.contains(paramDef.findFirstToken(TYPE)
                                           .getFirstChild()
                                           .getType());

    }
}
