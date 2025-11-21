import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;
import java.util.HashMap;

public class ExpressionParser {

    private String expression;
    private int position;
    private final Map<String, Complex> variables;
    private final Map<String, Complex> allVariables;

    private Node root;
    private Complex lastResult;

    private static class Node {
        String value;
        Node left, right;

        Node(String value) { this.value = value; }
        Node(String value, Node left, Node right) {
            this.value = value;
            this.left = left;
            this.right = right;
        }

        public String toString() {
            return value;
        }
    }

    public ExpressionParser(String expression, Map<String, Complex> variables) {
        this.expression = preprocess(expression.replaceAll("\\s+", ""));
        this.position = 0;
        this.variables = variables == null ? new HashMap<>() : variables;

        this.allVariables = new HashMap<>(this.variables);
        this.allVariables.put("i", new Complex(0, 1));
    }

    private String preprocess(String expr) {
        if (expr == null || expr.isEmpty()) return "";
        StringBuilder processed = new StringBuilder();
        for (int i = 0; i < expr.length() - 1; i++) {
            char c1 = expr.charAt(i);
            char c2 = expr.charAt(i + 1);

            processed.append(c1);
            if (needsMultiplication(c1, c2)) {
                processed.append('*');
            }
        }
        processed.append(expr.charAt(expr.length() - 1));
        return processed.toString();
    }

    private boolean needsMultiplication(char c1, char c2) {
        return (Character.isDigit(c1) && (Character.isLetter(c2) || c2 == '(')) ||
                (c1 == ')' && (Character.isDigit(c2) || Character.isLetter(c2) || c2 == '(')) ||
                (Character.isLetter(c1) && (Character.isDigit(c2) || c2 == '('));
    }

    public Complex evaluate() {
        root = null;
        position = 0;
        lastResult = evaluateAdditionSubtraction();

        if (position != expression.length()) {
            throw new IllegalArgumentException("Erro ao analisar expressão próximo de: " + expression.substring(position));
        }

        return lastResult;
    }

    private Node makeNode(String value, Node left, Node right) {
        return new Node(value, left, right);
    }

    private Complex evaluateAdditionSubtraction() {
        Node leftNode = null;
        Complex result = evaluateMultiplicationDivision();
        leftNode = root;

        while (position < expression.length()) {
            char operator = expression.charAt(position);
            if (operator == '+' || operator == '-') {
                position++;
                Complex right = evaluateMultiplicationDivision();
                Node rightNode = root;

                root = makeNode(String.valueOf(operator), leftNode, rightNode);

                if (operator == '+') result = result.plus(right);
                else result = result.minus(right);

                leftNode = root;
            } else break;
        }
        return result;
    }

    private Complex evaluateMultiplicationDivision() {
        Node leftNode = null;
        Complex result = evaluatePower();
        leftNode = root;

        while (position < expression.length()) {
            char operator = expression.charAt(position);
            if (operator == '*' || operator == '/') {
                position++;
                Complex right = evaluatePower();
                Node rightNode = root;

                root = makeNode(String.valueOf(operator), leftNode, rightNode);

                if (operator == '*') result = result.times(right);
                else result = result.divide(right);

                leftNode = root;
            } else break;
        }
        return result;
    }

    private Complex evaluatePower() {
        Complex result = evaluatePrimary();
        Node baseNode = root;

        while (position < expression.length() && expression.charAt(position) == '^') {
            position++;
            Complex right = evaluatePrimary();
            Node expNode = root;

            if (right.getImag() != 0)
                throw new IllegalArgumentException("Expoente da potência deve ser real.");

            result = result.pow(right.getReal());
            root = makeNode("^", baseNode, expNode);
            baseNode = root;
        }
        return result;
    }

    private Complex evaluatePrimary() {
        return evaluateUnitary();
    }

    private Complex evaluateUnitary() {
        boolean isNegative = false;

        if (position < expression.length() && expression.charAt(position) == '-') {
            isNegative = true;
            position++;
        }

        Complex result;

        if (expression.substring(position).startsWith("√")) {
            position++;
            result = evaluateUnitary();
            Node child = root;
            result = result.pow(0.5);
            root = new Node("√", child, null);
        }
        else if (position < expression.length() && expression.charAt(position) == '(') {
            int savePos = position;
            int start = position + 1;
            int balance = 1;
            int end = -1;
            for (int i = start; i < expression.length(); i++) {
                char c = expression.charAt(i);
                if (c == '(') balance++;
                if (c == ')') balance--;
                if (balance == 0) {
                    end = i;
                    break;
                }
            }

            if (end != -1) {
                String content = expression.substring(start, end);
                try {
                    Complex lit = Complex.parse(content);
                    position = end + 1;
                    result = lit;
                    root = new Node(content);
                } catch (Exception ex) {
                    position = savePos;
                    position++; // consome '('
                    result = evaluateAdditionSubtraction();
                    if (position < expression.length() && expression.charAt(position) == ')') position++;
                    else throw new IllegalArgumentException("Parênteses não fechados.");
                }
            } else {
                throw new IllegalArgumentException("Parênteses não fechados.");
            }
        }
        else if (position < expression.length() && Character.isLetter(expression.charAt(position))) {
            int start = position;
            while (position < expression.length() && Character.isLetter(expression.charAt(position)))
                position++;

            String varName = expression.substring(start, position);
            if (!allVariables.containsKey(varName))
                throw new IllegalArgumentException("Variável desconhecida: " + varName);

            result = allVariables.get(varName);
            root = new Node(varName);
        }
        else {
            int start = position;
            while (position < expression.length() &&
                    (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.'))
                position++;

            String num = expression.substring(start, position);
            if (num.isEmpty()) throw new IllegalArgumentException("Número esperado.");

            result = new Complex(Double.parseDouble(num), 0);
            root = new Node(num);
        }

        if (isNegative) {
            result = result.scale(-1);
            root = makeNode("-", new Node("0"), root);
        }

        return result;
    }

    public DefaultMutableTreeNode getExecutionTree() {
        String resultLabel = (lastResult != null) ? "Resultado: " + lastResult.toString() : "Resultado: (vazio)";
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(resultLabel);

        DefaultMutableTreeNode exprTree = buildSwingTree(root);
        top.add(exprTree);

        return top;
    }

    private DefaultMutableTreeNode buildSwingTree(Node n) {
        if (n == null) return new DefaultMutableTreeNode("vazio");

        String label = n.value;

        if (variables != null && variables.containsKey(n.value)) {
            Complex val = variables.get(n.value);
            label = n.value + " = " + val.toString();
        } else {
            try {
                Complex c = Complex.parse(n.value);
                label = c.toString();
            } catch (Exception ignored) {

            }
        }

        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(label);

        if (n.left != null) treeNode.add(buildSwingTree(n.left));
        if (n.right != null) treeNode.add(buildSwingTree(n.right));

        return treeNode;
    }

    public Node getAstRoot() {
        return root;
    }

    public Complex getLastResult() {
        return lastResult;
    }
}
