import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;

public class ExpressionParser {

    private String expression;
    private int position;
    private final Map<String, Complex> variables;

    private final Map<String, Complex> allVariables;

    public ExpressionParser(String expression, Map<String, Complex> variables) {
        this.expression = expression.replaceAll("\\s+", "");
        this.position = 0;
        this.variables = variables;

        this.allVariables = new HashMap<>(variables);
        this.allVariables.put("i", new Complex(0, 1));
    }

    public Complex evaluate() {
        return evaluateAdditionSubtraction();
    }


    private Complex parseComplexNumberLiteral() {
        if (position >= expression.length() || expression.charAt(position) != '(') {
            throw new IllegalArgumentException("Parênteses de número complexo esperado.");
        }

        int start = position + 1;
        int end = -1;
        int balance = 1;

        for (int i = start; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') balance++;
            if (c == ')') balance--;
            if (balance == 0) {
                end = i;
                break;
            }
        }

        if (end == -1) {
            throw new IllegalArgumentException("Parênteses do número complexo não fechados.");
        }

        String content = expression.substring(start, end);
        position = end + 1;

        try {
            return Complex.parse(content);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Formato do número complexo inválido dentro de parênteses: " + content);
        }
    }



    private Complex evaluateAdditionSubtraction() {
        Complex result = evaluateMultiplicationDivision();
        while (position < expression.length()) {
            char operator = expression.charAt(position);
            if (operator == '+' || operator == '-') {
                position++;
                Complex right = evaluateMultiplicationDivision();
                if (operator == '+') {
                    result = result.plus(right);
                } else {
                    result = result.minus(right);
                }
            } else {
                break;
            }
        }
        return result;
    }


    private Complex evaluateMultiplicationDivision() {
        Complex result = evaluatePower();
        while (position < expression.length()) {
            char operator = expression.charAt(position);
            if (operator == '*' || operator == '/') {
                position++;
                Complex right = evaluatePower();
                if (operator == '*') {
                    result = result.times(right);
                } else {
                    result = result.divide(right);
                }
            } else {
                break;
            }
        }
        return result;
    }


    private Complex evaluatePower() {
        Complex result = evaluatePrimary();
        while (position < expression.length() && expression.charAt(position) == '^') {
            position++;
            Complex right = evaluatePrimary();

            if (right.getImag() != 0) {
                throw new IllegalArgumentException("Expoente da Potência deve ser um número real.");
            }

            result = result.pow(right.getReal());
        }
        return result;
    }



    private Complex evaluatePrimary() {
        return evaluateUnitary();
    }


    private Complex evaluateUnitary() {
        boolean isNegative = false;
        Complex result;
        int start;

        if (position < expression.length() && expression.charAt(position) == '-') {
            isNegative = true;
            position++;
        }

        if (expression.substring(position).startsWith("√")) {
            position++;
            result = evaluateUnitary();
            result = result.pow(0.5);
        }
        else if (position < expression.length() && expression.charAt(position) == '(') {

            int tempPosition = position;
            try {
                result = parseComplexNumberLiteral();
            } catch (IllegalArgumentException complexEx) {
                position = tempPosition;
                position++;

                result = evaluateAdditionSubtraction();

                if (position < expression.length() && expression.charAt(position) == ')') {
                    position++;
                } else {
                    throw new IllegalArgumentException("Parênteses da expressão não fechados.");
                }
            }
        }
        else if (position < expression.length() && Character.isLetter(expression.charAt(position))) {

            start = position;
            while (position < expression.length() && Character.isLetter(expression.charAt(position))) {
                position++;
            }
            String varName = expression.substring(start, position);

            if (allVariables.containsKey(varName)) {
                result = allVariables.get(varName);
            } else {
                throw new IllegalArgumentException("Valor da variável '" + varName + "' não fornecido ou variável desconhecida.");
            }
        }
        else {
            start = position;
            while (position < expression.length() && (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.')) {
                position++;
            }

            String numberStr = expression.substring(start, position);
            if (numberStr.isEmpty()) {
                throw new IllegalArgumentException("Operando esperado.");
            }

            try {
                double value = Double.parseDouble(numberStr);
                result = new Complex(value, 0);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Erro de representação do número real.");
            }
        }

        if (isNegative) {
            return result.scale(-1);
        }
        return result;
    }
}