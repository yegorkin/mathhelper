package ua.kiev.splash.mathhelper.calculator;

import ua.kiev.splash.mathhelper.api.Evaluator;
import ua.kiev.splash.mathhelper.exceptions.EvaluationException;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom expression evaluation engine instead of ScriptEngineManager or ExpressionBuilder
 */
public class HandMadeEvaluator implements Evaluator {
    private static final Logger log = LoggerFactory.getLogger(HandMadeEvaluator.class);

    private static final char OP_PARENTHESIS_OPEN = '(';
    private static final char OP_PARENTHESIS_CLOSE = ')';
    private static final char OP_PLUS = '+';
    private static final char OP_MINUS = '-';
    private static final char OP_MULTIPLY = '*';
    private static final char OP_DIVIDE = '/';
    private static final char DECIMAL_POINT = '.';
    private static final char OP_UNARY_MINUS = '~';
    private static final String OP_EQUALS = "=";

    private static final String PARENTHESIS_OPEN_STR = String.valueOf(OP_PARENTHESIS_OPEN);
    private static final String PARENTHESIS_CLOSE_STR = String.valueOf(OP_PARENTHESIS_CLOSE);

    private enum Operator {
        PARENTHESIS_OPEN(PARENTHESIS_OPEN_STR, 0),
        PARENTHESIS_CLOSE(PARENTHESIS_CLOSE_STR, 0),
        ADD(String.valueOf(OP_PLUS), 1),
        SUB(String.valueOf(OP_MINUS), 1),
        MUL(String.valueOf(OP_MULTIPLY), 2),
        DIV(String.valueOf(OP_DIVIDE), 2),
        NEG(String.valueOf(OP_UNARY_MINUS), 3);

        private final String name;
        private final int priority;

        Operator(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        public String getName() {
            return name;
        }

        public int getPriority() {
            return priority;
        }
    }

    private static final Map<String, Operator> operators;
    static {
        operators = new HashMap<>();
        for (Operator operator : Operator.values()) {
            operators.put(operator.getName().toUpperCase(), operator); // named operators like "add" are possible
        }
    }

    @Override
    public double evaluateExpression(String expressionText, Map<String, Double> parameters) {
        return evaluateExpression(expressionText, parameters, false);
    }

    @Override
    public boolean checkEquationEquality(String equationText, Map<String, Double> parameters) {
        return checkEquationEquality(equationText, parameters, Evaluator.DEFAULT_EPSILON, false);
    }

    @Override
    public boolean checkEquationEquality(String equationText, Map<String, Double> parameters, double epsilon) {
        return checkEquationEquality(equationText, parameters, epsilon, false);
    }

    @Override
    public void validateEquationSyntax(String equationText, Set<String> parameters) {
        Map<String, Double> dryRunParameters = parameters.stream().collect(Collectors.toMap(e -> e, e -> 1d));
        checkEquationEquality(equationText, dryRunParameters, DEFAULT_EPSILON, true);
    }

    private boolean checkEquationEquality(String equationText, Map<String, Double> parameters, double epsilon, boolean isDryRun) {
        log.debug("checkEquationEquality(equationText = \"{}\", parameters = {}, epsilon = {})", equationText, parameters, epsilon);
        if (isStringBlank(equationText)) {
            throw new EvaluationException("Порожнє рівняння");
        }
        String[] equationParts = equationText.split(OP_EQUALS, -1 /* do not skip empty parts */);
        if (equationParts.length != 2) {
            throw new EvaluationException("Рівняння повинно складатися з лівої і правої частини - містити один знак \"" +
                    OP_EQUALS + "\"");
        }
        String leftEquationPart = equationParts[0];
        if (isStringBlank(leftEquationPart)) {
            throw new EvaluationException("Порожня ліва частина рівняння");
        }
        String rightEquationPart = equationParts[1];
        if (isStringBlank(rightEquationPart)) {
            throw new EvaluationException("Порожня права частина рівняння");
        }

        double leftValue = evaluateExpression(leftEquationPart, parameters, isDryRun);
        log.debug("Left equation part result = {}", leftValue);
        double rightValue = evaluateExpression(rightEquationPart, parameters, isDryRun);
        log.debug("Right equation part result = {}", rightValue);

        boolean result = Math.abs(leftValue - rightValue) < epsilon;
        log.debug("Equation check result: {}", result);

        return result;
    }

    private double evaluateExpression(String expressionText, Map<String, Double> parameters, boolean isDryRun) {
        log.debug("expressionText = \"{}\"", expressionText);
        List<String> lexemes = preprocessExpressionText(expressionText);
        log.debug("lexemes = {}", lexemes);
        List<String> postfixLexemes;
        try {
            postfixLexemes = convertToPostfixNotation(lexemes);
        } catch (EmptyStackException ese) {
            throw new EvaluationException("Неправильний вираз (перевірте дужки): \"" + expressionText + "\"");
        }
        log.debug("postfixLexemes = {}", postfixLexemes);
        double result;
        try {
            result = evaluatePostfixExpression(postfixLexemes, parameters, isDryRun);
        } catch (EmptyStackException ese) {
            throw new EvaluationException("Неправильний вираз: \"" + expressionText + "\"");
        }
        log.debug("result = {}", result);
        return result;
    }

    private List<String> preprocessExpressionText(String expressionText) {
        // no need to explicitly validate input for OP_UNARY_MINUS because it is not a part of public grammar
        List<String> result = new ArrayList<>();
        char[] inputChars = expressionText.toCharArray();
        int lexemeLength = 0;
        int lexemeStart = -1;

        boolean isNextUnaryMinus = true;
        for (int i = 0; i < inputChars.length; i++) {
            char ch = inputChars[i];

            if (lexemeLength != 0) {
                if (Character.isLetterOrDigit(ch) || ch == DECIMAL_POINT) {
                    lexemeLength++;
                    continue;
                } else {
                    addLexeme(inputChars, lexemeStart, lexemeLength, result);
                    lexemeLength = 0;
                    isNextUnaryMinus = false;
                }
            } else if (Character.isLetterOrDigit(ch) || ch == DECIMAL_POINT) {
                lexemeStart = i;
                lexemeLength = 1;
                continue;
            }

            // operator symbols processing
            if (Character.isWhitespace(ch)) {
                continue; // do nothing
            } else if (isParenthesis(ch)) {
                addLexeme(inputChars, i, 1, result);
                isNextUnaryMinus = OP_PARENTHESIS_CLOSE != ch;
            } else if (isOperator(ch)) {
                if (isNextUnaryMinus && OP_MINUS == ch) {
                    addUnaryMinusLexeme(result);
                } else {
                    addLexeme(inputChars, i, 1, result);
                }
                isNextUnaryMinus = true;
            } else {
                throw new EvaluationException("Неприпустимий символ \"" + ch + "\" в позиції " + (i + 1) + " у виразі: \"" + expressionText + "\"");
            }
        }
        if (lexemeLength != 0) {
            addLexeme(inputChars, lexemeStart, lexemeLength, result);
        }
        return result;
    }

    private void addLexeme(char[] inputChars, int lexemeStart, int lexemeLength, List<String> lexemes) {
        lexemes.add(new String(inputChars, lexemeStart, lexemeLength));
    }

    private void addUnaryMinusLexeme(List<String> lexemes) {
        lexemes.add(Operator.NEG.getName().toUpperCase());
    }

    private boolean isParenthesis(char ch) {
        return ch == '(' || ch == ')';
    }

    private boolean isOperator(char ch) {
        return ch == OP_PLUS || ch == OP_MINUS || ch == OP_MULTIPLY || ch == OP_DIVIDE;
    }

    private Operator lookupOperator(String name) {
        return operators.get(name.toUpperCase());
    }

    private boolean isStringBlank(String s) {
        return s == null || s.isBlank();
    }

    private List<String> convertToPostfixNotation(List<String> lexemes) {
        List<String> result = new ArrayList<>();
        Stack<String> stack = new Stack<>();
        for (String lexeme : lexemes) {
            Operator operator = lookupOperator(lexeme);
            if (operator != null) {
                if (!stack.isEmpty() && !PARENTHESIS_OPEN_STR.equals(lexeme)) {
                    if (PARENTHESIS_CLOSE_STR.equals(lexeme)) {
                        // closing parenthesis pops all operations from the stack up to the nearest opening parenthesis
                        String s = stack.pop();
                        while (!PARENTHESIS_OPEN_STR.equals(s)) {
                            result.add(s);
                            s = stack.pop();
                        }
                    } else {
                        // operation pops all operations with greater or equal priority from the stack
                        while (!stack.isEmpty() && operator.getPriority() <= lookupOperator(stack.peek()).getPriority()) {
                            result.add(stack.pop());
                        }
                        stack.push(lexeme);
                    }
                } else {
                    stack.push(lexeme); // open parenthesis or stack is empty
                }
            } else {
                result.add(lexeme);
            }
        }
        while (!stack.empty()) {
            result.add(stack.pop());
        }
        return result;
    }

    private double evaluatePostfixExpression(List<String> postfixLexemes, Map<String, Double> parameters, boolean isDryRun) {
        double a;
        Stack<Double> stack = new Stack<>();
        for (String lexeme : postfixLexemes) {
            Operator operator = lookupOperator(lexeme);
            if (operator != null) {
                switch (operator) {
                    case ADD:
                        stack.push(stack.pop() + stack.pop());
                        break;
                    case SUB:
                        a = stack.pop();
                        stack.push(stack.pop() - a);
                        break;
                    case MUL:
                        stack.push(stack.pop() * stack.pop());
                        break;
                    case DIV:
                        a = stack.pop();
                        if (isDryRun) {
                            a = 1; // avoid Infinity division result in dry run mode
                        }
                        stack.push(stack.pop() / a); // division by zero returns Infinity (INF)
                        break;
                    case NEG:
                        stack.push(-stack.pop());
                        break;
                    default:
                        throw new EvaluationException("Unsupported operator encountered: " + operator.getName());
                }
                continue;
            }
            Double parameterValue = parameters.get(lexeme);
            if (parameterValue != null) {
                stack.push(parameterValue);
                continue;
            }
            try {
                stack.push(Double.parseDouble(lexeme));
            } catch (NumberFormatException nfe) {
                throw new EvaluationException("Неправильний операнд виразу: \"" + lexeme + "\"");
            }
        }
        double result = stack.pop();
        if (!stack.isEmpty()) {
            throw new EvaluationException("Неправильний вираз (залишаються необроблені дані)"); // stack should be empty but it is not
        }
        return result;
    }
}
