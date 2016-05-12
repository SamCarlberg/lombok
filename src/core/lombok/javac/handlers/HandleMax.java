package lombok.javac.handlers;

import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import lombok.ConfigurationKeys;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.experimental.Max;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import org.mangosdk.spi.ProviderFor;

import static lombok.core.handlers.HandlerUtil.*;
import static lombok.javac.Javac.*;
import static lombok.javac.JavacTreeMaker.TreeTag.*;
import static lombok.javac.JavacTreeMaker.TypeTag.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

@ProviderFor(JavacAnnotationHandler.class)
@HandlerPriority(1024) // 2^10 -- lower priority than HandleRange (@Range overrides @Max)
public class HandleMax extends JavacAnnotationHandler<Max> {

    private static final String numberTypePattern = "^(byte|Byte|char|Character|int|Integer|long|Long|float|Float|double|Double)$";

    @Override
    public void handle(AnnotationValues<Max> annotation, JCAnnotation ast, JavacNode annotationNode) {
        handleFlagUsage(annotationNode, ConfigurationKeys.MIN_FLAG_USAGE, "@Max");
        if (annotationNode.up().getKind() == AST.Kind.FIELD) {
            try {
                if (!((JCVariableDecl) annotationNode.up().get()).vartype.toString().matches(numberTypePattern)) {
                    annotationNode.addError("@Max can only be used on numbers");
                }
            } catch (Exception ignore) {
            }
            return;
        }
        if (annotationNode.up().getKind() != AST.Kind.ARGUMENT) return;

        try {
            if (!((JCVariableDecl) annotationNode.up().get()).vartype.toString().matches(numberTypePattern)) {
                annotationNode.addError("@Max can only be used on numbers");
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

        if (declaration.body == null) {
            // Since @Max can be used for documentation purposes, don't emit an error or warning
            return;
        }

        double max = annotation.getInstance().value();
        JCStatement maxCheck = recursiveSetGeneratedBy(generateMaxCheck(annotationNode.getTreeMaker(), annotationNode.up(), max), ast, annotationNode.getContext());

        if (maxCheck == null) {
            return;
        }

        List<JCStatement> statements = declaration.body.stats;

        String expectedName = annotationNode.up().getName();

		    /* Abort if the max check is already there, delving into try and synchronized statements */
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
                String varNameOfMaxCheck = returnVarNameIfMaxCheck(stat);
                if (varNameOfMaxCheck == null) {
                    continue;
                }
                if (varNameOfMaxCheck.equals(expectedName)) {
                    annotationNode.addWarning("Not generating upper bound check, one is already present");
                    return;
                }
            }
        }

        List<JCStatement> tail = statements;
        List<JCStatement> head = List.nil();
        for (JCStatement stat : statements) {
            if (JavacHandlerUtil.isConstructorCall(stat)) {
                tail = tail.tail;
                head = head.prepend(stat);
                continue;
            }
            if (expectedName.equals(returnVarNameIfMaxCheck(stat))) {
                annotationNode.addWarning("Not generating upper bound check, one is already present");
                return;
            }
        }

        List<JCStatement> newList = tail.prepend(maxCheck);
        for (JCStatement stat : head) newList = newList.prepend(stat);
        declaration.body.stats = newList;
        annotationNode.getAst().setChanged();
    }

    /**
     * Checks if the statement is of the form 'if (x > y) {throw WHATEVER;},
     * where the block braces are optional. If it is of this form, returns "x".
     * If it is not of this form, returns null.
     */
    static String returnVarNameIfMaxCheck(JCStatement stat) {
        if (!(stat instanceof JCIf)) return null;

		    /* Check that the if's statement is a throw statement, possibly in a block. */
        {
            JCStatement then = ((JCIf) stat).thenpart;
            if (then instanceof JCBlock) {
                List<JCStatement> stats = ((JCBlock) then).stats;
                if (stats.length() == 0) return null;
                then = stats.get(0);
            }
            if (!(then instanceof JCThrow)) return null;
        }

		/*
           Check that the if's conditional is like 'x > y'. Return from this method (don't generate
		   a max check) if 'x' is equal to our own variable's name: There's already a max check here.
		 */
        {
            JCExpression cond = ((JCIf) stat).cond;
            while (cond instanceof JCParens) cond = ((JCParens) cond).expr;
            if (!(cond instanceof JCBinary)) return null;
            JCBinary bin = (JCBinary) cond;
            if (!CTC_GREATER_THAN.equals(treeTag(bin))) return null;
            if (!(bin.lhs instanceof JCIdent)) return null;
            if (!(bin.rhs instanceof JCLiteral)) return null;
            if (!CTC_DOUBLE.equals(typeTag(bin.rhs))) return null;
            return ((JCIdent) bin.lhs).name.toString();
        }
    }
}
