package ua.kiev.splash.mathhelper.api;

import java.util.Map;
import java.util.Set;

public interface Evaluator {
    double DEFAULT_EPSILON = 10E-9; // permissible calculation error
    String DEFAULT_ROOT_NAME = "x";

    /**
     * Checks equation equality. Equation example: "1 + 2 * (3 - x) = 4 / x".
     * @see #checkEquationEquality(String, Map, double)
     * @param equationText equation text
     * @param parameters equation parameters
     * @return True if the left and right sides of the equation are equal and False otherwise.
     */
    boolean checkEquationEquality(String equationText, Map<String, Double> parameters);

    /**
     * Checks equation equality. Equation example: "1 + 2 * (3 - x) = 4 / x".
     * @param equationText equation text
     * @param parameters equation parameters
     * @param epsilon permissible calculation error
     * @return True if the left and right sides of the equation are equal and False otherwise.
     */
    boolean checkEquationEquality(String equationText, Map<String, Double> parameters, double epsilon);

    /**
     * Validates equation syntax. Equation example: "1 + 2 * (3 - x) = 4 / x".
     * @param equationText equation text
     * @param parameters equation parameters
     */
    void validateEquationSyntax(String equationText, Set<String> parameters);

    /**
     * Evaluates expressions like "2 + 2 + x"
     *
     * @param expressionText expression text
     * @param parameters expression parameters
     * @return evaluation result
     */
    double evaluateExpression(String expressionText, Map<String, Double> parameters);
}
