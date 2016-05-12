package lombok.javac.handlers;

import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import lombok.ConfigurationKeys;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.experimental.Range;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import org.mangosdk.spi.ProviderFor;

import static lombok.core.handlers.HandlerUtil.handleFlagUsage;
import static lombok.javac.handlers.HandleMax.returnVarNameIfMaxCheck;
import static lombok.javac.handlers.HandleMin.returnVarNameIfMinCheck;
import static lombok.javac.handlers.JavacHandlerUtil.*;

@ProviderFor(JavacAnnotationHandler.class)
@HandlerPriority(512) // 2^9 -- same priority as HandleNonNull
public class HandleRange extends JavacAnnotationHandler<Range> {

    private static final String numberTypePattern = "^(byte|Byte|char|Character|int|Integer|long|Long|float|Float|double|Double)$";

    @Override
    public void handle(AnnotationValues<Range> annotation, JCAnnotation ast, JavacNode annotationNode) {
        handleFlagUsage(annotationNode, ConfigurationKeys.MIN_FLAG_USAGE, "@Min");
        handleFlagUsage(annotationNode, ConfigurationKeys.MAX_FLAG_USAGE, "@Max");
        if (annotationNode.up().getKind() == AST.Kind.FIELD) {
            try {
                if (!((JCVariableDecl) annotationNode.up().get()).vartype.toString().matches(numberTypePattern)) {
                    annotationNode.addError("@Range can only be used on numbers");
                }
            } catch (Exception ignore) {
            }
            return;
        }
        if (annotationNode.up().getKind() != AST.Kind.ARGUMENT) return;

        try {
            if (!((JCVariableDecl) annotationNode.up().get()).vartype.toString().matches(numberTypePattern)) {
                annotationNode.addError("@Range can only be used on numbers");
                return;
            }
        } catch (Exception ignore) {
            return;
        }

        JCMethodDecl declaration;

        try {
            declaration = (JCMethodDecl) annotationNode.up().up().get();
        } catch (Exception e) {
            return;
        }

        double[] range = annotation.getInstance().value();
        if (range.length != 2) {
            annotationNode.addError(String.format("Must have exactly two values for @Range (given: %d)", range.length));
            return;
        }
        if (range[0] >= range[1]) {
            annotationNode.addError("Min value must be less than max value");
            return;
        }
        JCStatement minCheck = recursiveSetGeneratedBy(generateMinCheck(annotationNode.getTreeMaker(), annotationNode.up(), range[0]), ast, annotationNode.getContext());
        JCStatement maxCheck = recursiveSetGeneratedBy(generateMaxCheck(annotationNode.getTreeMaker(), annotationNode.up(), range[1]), ast, annotationNode.getContext());

        if (minCheck == null) {
            return;
        }
        if (maxCheck == null) {
            return;
        }

        boolean hasMin = false, hasMax = false;

        List<JCStatement> statements = declaration.body.stats;

        String expectedName = annotationNode.up().getName();

		    /* Don't generate min or max check is one is already there, delving into try and synchronized statements */
        {
            List<JCStatement> stats = statements;
            int idx = 0;
            while (stats.size() > idx) {
                JCStatement stat = stats.get(idx++);
                if (JavacHandlerUtil.isConstructorCall(stat)) continue;
                if (stat instanceof JCTry) {
                    stats = ((JCTry) stat).body.stats;
                    idx = 0;
                    continue;
                }
                if (stat instanceof JCSynchronized) {
                    stats = ((JCSynchronized) stat).body.stats;
                    idx = 0;
                    continue;
                }
                String varNameOfMinCheck = returnVarNameIfMinCheck(stat);
                String varNameOfMaxCheck = returnVarNameIfMaxCheck(stat);
                if (expectedName.equals(varNameOfMinCheck)) {
                    annotationNode.addWarning("Not generating lower bound check, one is already present");
                    hasMin = true;
                    break;
                }
                if (expectedName.equals(varNameOfMaxCheck)) {
                    annotationNode.addWarning("Not generating upper bound check, one is already present");
                    hasMax = true;
                    break;
                }
            }
        }

        List<JCStatement> tail = statements;
        List<JCStatement> head = List.nil();
        for (JCStatement stat : statements) {
            if (JavacHandlerUtil.isConstructorCall(stat) || JavacHandlerUtil.isGenerated(stat)) {
                tail = tail.tail;
                head = head.prepend(stat);
            }
            if (expectedName.equals(returnVarNameIfMinCheck(stat)) && !hasMin) {
                annotationNode.addWarning("Not generating lower bound check, one is already present");
                hasMin = true;
            }
            if (expectedName.equals(returnVarNameIfMaxCheck(stat)) && !hasMax) {
                annotationNode.addWarning("Not generating upper bound check, one is already present");
                hasMax = true;
            }
        }

        List<JCStatement> newList = tail;
        if (!hasMax) newList = newList.prepend(maxCheck);
        if (!hasMin) newList = newList.prepend(minCheck);
        for (JCStatement stat : head) newList = newList.prepend(stat);
        declaration.body.stats = newList;
        annotationNode.getAst().setChanged();
    }


}
