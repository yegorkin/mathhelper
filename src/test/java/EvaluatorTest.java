import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.kiev.splash.mathhelper.api.Evaluator;
import ua.kiev.splash.mathhelper.calculator.HandMadeEvaluator;
import ua.kiev.splash.mathhelper.exceptions.EvaluationException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Test only the most fragile and important part. But this test shows that I actually can create tests.
public class EvaluatorTest {
    private static final Map<String, Double> X_EQUALS_1 = Map.of(Evaluator.DEFAULT_ROOT_NAME, 1d);
    private static final Set<String> X_PARAM = Collections.singleton(Evaluator.DEFAULT_ROOT_NAME);
    private Evaluator evaluator;

    @BeforeAll
    static void setup() {
        // something to set up
    }

    @BeforeEach
    void init() {
        evaluator = new HandMadeEvaluator();
    }

    @Test
    void testCheckEquationEquality() {
        // tests from the original task
        assertTrue(evaluator.checkEquationEquality("2*x+5=17", Map.of(Evaluator.DEFAULT_ROOT_NAME, 6d)));
        assertTrue(evaluator.checkEquationEquality("-1.3*5/x=1.2", Map.of(Evaluator.DEFAULT_ROOT_NAME, -5.4166666666666666666666666666667)));
        assertTrue(evaluator.checkEquationEquality("2*x*x=10", Map.of(Evaluator.DEFAULT_ROOT_NAME, 2.2360679774997896964091736687313)));
        assertTrue(evaluator.checkEquationEquality("2*(x+5+x)+5=10", Map.of(Evaluator.DEFAULT_ROOT_NAME, -1.25)));
        assertTrue(evaluator.checkEquationEquality("17=2*x+5", Map.of(Evaluator.DEFAULT_ROOT_NAME, 6d)));

        // some other tests
        assertTrue(evaluator.checkEquationEquality("1 = 1", X_EQUALS_1));
        assertTrue(evaluator.checkEquationEquality("x = 1", X_EQUALS_1));
        assertTrue(evaluator.checkEquationEquality("1 = x", X_EQUALS_1));
        assertTrue(evaluator.checkEquationEquality("x = x", X_EQUALS_1));
        assertTrue(evaluator.checkEquationEquality("-x + 2 = 1", X_EQUALS_1));
        assertTrue(evaluator.checkEquationEquality("x * -1 = -1", X_EQUALS_1));
        assertTrue(evaluator.checkEquationEquality("2 + 2 * 2 = x + 5", X_EQUALS_1));
        assertTrue(evaluator.checkEquationEquality("-(-x + 2) = x - 2", X_EQUALS_1));

        // tests for false
        assertFalse(evaluator.checkEquationEquality("1 = 2", X_EQUALS_1));
        assertFalse(evaluator.checkEquationEquality("x = 2", X_EQUALS_1));
    }

    @Test
    void testValidateEquationSyntax() {
        assertDoesNotThrow(() -> evaluator.validateEquationSyntax("(1 * 2) = x", X_PARAM));
        assertDoesNotThrow(() -> evaluator.validateEquationSyntax("(1 * 2) = (x - 1) * (x + 1)", X_PARAM));

        assertThrows(EvaluationException.class, () -> evaluator.validateEquationSyntax("", X_PARAM));
        assertThrows(EvaluationException.class, () -> evaluator.validateEquationSyntax("=", X_PARAM));
        assertThrows(EvaluationException.class, () -> evaluator.validateEquationSyntax("x =", X_PARAM));
        assertThrows(EvaluationException.class, () -> evaluator.validateEquationSyntax("1=1=", X_PARAM));
        assertThrows(EvaluationException.class, () -> evaluator.validateEquationSyntax("1 - * 2 = x", X_PARAM));
        assertThrows(EvaluationException.class, () -> evaluator.validateEquationSyntax("(1 * 2 = x", X_PARAM));
        assertThrows(EvaluationException.class, () -> evaluator.validateEquationSyntax("(1 * 2 = x)", X_PARAM));
    }

    @Test
    void testEvaluateExpression() {
        assertTrue(Math.abs(evaluator.evaluateExpression("x", X_EQUALS_1) - 1d) < Evaluator.DEFAULT_EPSILON);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testEvaluatorConstants() {
        assertTrue(Evaluator.DEFAULT_EPSILON != 0);
        assertEquals(10E-9, Evaluator.DEFAULT_EPSILON);
    }

//    @Test
//    @Disabled("Not implemented yet")
//    void testUsingConsoleApplication() {
//    }
}
