package com.jetbrains.python.inspection;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PyConstantExpression extends PyInspection {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly,
                                          @NotNull LocalInspectionToolSession session) {
        return new Visitor(holder, session);
    }

    private static class Visitor extends PyInspectionVisitor {

        private Visitor(@Nullable ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
            super(holder, session);
        }

        @Override
        public void visitPyIfStatement(PyIfStatement node) {
            super.visitPyIfStatement(node);
            processIfPart(node.getIfPart());
            for (PyIfPart part : node.getElifParts()) {
                processIfPart(part);
            }
        }

        private void processIfPart(@NotNull PyIfPart pyIfPart) {
            final PyExpression condition = pyIfPart.getCondition();
            if (condition instanceof PyBoolLiteralExpression) {
                registerProblem(condition, "The condition is always " + ((PyBoolLiteralExpression) condition).getValue());
            }

            if (condition instanceof PyBinaryExpression) {
                PyBinaryExpression binaryExpression = (PyBinaryExpression) condition;
                PyExpression leftOperand = binaryExpression.getLeftExpression();
                PyExpression rightOperand = binaryExpression.getRightExpression();
                PyElementType operator = binaryExpression.getOperator();

                if ((operator == PyTokenTypes.LT || operator == PyTokenTypes.GT
                        || operator == PyTokenTypes.EQEQ || operator == PyTokenTypes.NE
                        || operator == PyTokenTypes.LE || operator == PyTokenTypes.GE)
                        && leftOperand instanceof PyNumericLiteralExpression
                        && rightOperand instanceof PyNumericLiteralExpression) {

                    Long leftOperandValue = ((PyNumericLiteralExpression) leftOperand).getLongValue();
                    Long rightOperandValue = ((PyNumericLiteralExpression) rightOperand).getLongValue();

                    if (leftOperandValue != null && rightOperandValue != null) {
                        boolean comparisonResult = false;

                        if (operator == PyTokenTypes.LT)
                            comparisonResult = leftOperandValue < rightOperandValue;

                        if (operator == PyTokenTypes.GT)
                            comparisonResult = leftOperandValue > rightOperandValue;

                        if (operator == PyTokenTypes.EQEQ)
                            comparisonResult = leftOperandValue.equals(rightOperandValue);

                        if (operator == PyTokenTypes.NE)
                            comparisonResult = !leftOperandValue.equals(rightOperandValue);

                        if (operator == PyTokenTypes.LE)
                            comparisonResult = leftOperandValue <= rightOperandValue;

                        if (operator == PyTokenTypes.GE)
                            comparisonResult = leftOperandValue >= rightOperandValue;

                        registerProblem(condition, "The condition is always " + comparisonResult);
                    }
                }
            }
        }
    }
}
