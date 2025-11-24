// ExpressionParser.java
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class ExpressionParser {

    private final String expression;
    private int pos;
    private final Map<String, Complex> variables;
    private final Map<String, Complex> allVariables;

    private Node root;
    private Complex lastResult;

    // ==============================
    // compareAst
    // ==============================
    public static boolean compareAst(Node a, Node b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (!a.token.equals(b.token)) return false;
        if (a.children.size() != b.children.size()) return false;

        for (int i = 0; i < a.children.size(); i++) {
            if (!compareAst(a.children.get(i), b.children.get(i)))
                return false;
        }
        return true;
    }

    // ==============================
    // NÓ DO AST
    // ==============================
    public static class Node {
        public final String token;
        public final java.util.List<Node> children;

        public Node(String token, Node... children) {
            this.token = token;
            this.children = new ArrayList<>();
            if (children != null)
                for (Node c : children)
                    this.children.add(c);
        }
    }

    private static class Result {
        Complex value;
        Node node;
        Result(Complex v, Node n) {
            value = v;
            node = n;
        }
    }

    public ExpressionParser(String expression) {
        this(expression, null);
    }

    public ExpressionParser(String expression, Map<String, Complex> variables) {
        if (expression == null)
            expression = "";

        this.expression = expression.replaceAll("\\s+", "");
        this.pos = 0;
        this.variables = variables == null ? new HashMap<>() : new HashMap<>(variables);

        this.allVariables = new HashMap<>(this.variables);
        this.allVariables.put("i", new Complex(0, 1));
    }

    private boolean isKnownFunction(String name) {
        return name.equalsIgnoreCase("sin") ||
                name.equalsIgnoreCase("cos") ||
                name.equalsIgnoreCase("tan") ||
                name.equalsIgnoreCase("log") ||
                name.equalsIgnoreCase("exp") ||
                name.equalsIgnoreCase("abs") ||
                name.equalsIgnoreCase("sqrt");
    }

    public Complex evaluate() {
        pos = 0;
        Result r = parseExpression();
        if (pos != expression.length())
            throw new IllegalArgumentException("Erro perto de: " + expression.substring(pos));

        root = r.node;
        lastResult = r.value;
        return lastResult;
    }

    public Node getAstRoot() {
        return root;
    }

    public DefaultMutableTreeNode getExecutionTree() {
        if (root == null)
            return new DefaultMutableTreeNode("Nenhuma expressão avaliada");

        return buildTree(root);
    }

    private DefaultMutableTreeNode buildTree(Node n) {
        DefaultMutableTreeNode dm = new DefaultMutableTreeNode(n.token + " = " + evaluateNode(n));

        for (Node c : n.children)
            dm.add(buildTree(c));

        return dm;
    }

    // ==============================
    // ÁRVORE LISP
    // ==============================
    public String getLispTree() {
        if (root == null)
            return "()";
        return toLisp(root);
    }

    private String toLisp(Node node) {
        if (node.children.isEmpty()) {
            return node.token;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("(").append(node.token);

        for (Node child : node.children) {
            sb.append(" ").append(toLisp(child));
        }

        sb.append(")");
        return sb.toString();
    }

    private String evaluateNode(Node node) {
        try {
            if (node.children.isEmpty()) {
                // É um número ou variável
                if (node.token.matches("-?\\d+(\\.\\d+)?")) {
                    return node.token;
                } else if (allVariables.containsKey(node.token)) {
                    return allVariables.get(node.token).toString();
                } else {
                    return node.token;
                }
            }

            // É uma operação
            switch (node.token) {
                case "+": return "soma";
                case "-": return "subtração";
                case "*": return "multiplicação";
                case "/": return "divisão";
                case "^": return "potência";
                case "√": return "raiz";
                case "sin": return "seno";
                case "cos": return "cosseno";
                case "tan": return "tangente";
                case "log": return "logaritmo";
                default: return node.token;
            }
        } catch (Exception e) {
            return "?";
        }
    }

    // ================================================
    // PARSER SIMPLIFICADO - SEM PREPROCESS
    // ================================================
    private Result parseExpression() {
        Result left = parseTerm();

        while (pos < expression.length()) {
            char op = expression.charAt(pos);
            if (op == '+' || op == '-') {
                pos++;
                Result right = parseTerm();
                Complex val = (op == '+') ? left.value.plus(right.value) : left.value.minus(right.value);
                Node node = new Node(String.valueOf(op), left.node, right.node);
                left = new Result(val, node);
            } else {
                break;
            }
        }
        return left;
    }

    private Result parseTerm() {
        Result left = parseFactor();

        while (pos < expression.length()) {
            char op = expression.charAt(pos);
            if (op == '*' || op == '/') {
                pos++;
                Result right = parseFactor();
                Complex val = (op == '*') ? left.value.times(right.value) : left.value.divide(right.value);
                Node node = new Node(String.valueOf(op), left.node, right.node);
                left = new Result(val, node);
            } else {
                break;
            }
        }
        return left;
    }

    private Result parseFactor() {
        // Verificar se é um sinal negativo
        if (pos < expression.length() && expression.charAt(pos) == '-') {
            pos++;
            Result r = parseFactor();
            Complex val = r.value.scale(-1);
            Node zero = new Node("0");
            Node node = new Node("-", zero, r.node);
            return new Result(val, node);
        }
        return parsePower();
    }

    private Result parsePower() {
        Result left = parsePrimary();

        if (pos < expression.length() && expression.charAt(pos) == '^') {
            pos++;
            Result right = parseFactor();

            if (right.value.getImag() != 0)
                throw new IllegalArgumentException("Expoente deve ser real.");

            Complex val = left.value.pow(right.value.getReal());
            Node node = new Node("^", left.node, right.node);
            return new Result(val, node);
        }
        return left;
    }

    private Result parsePrimary() {
        if (pos >= expression.length())
            throw new IllegalArgumentException("Expressão incompleta");

        char c = expression.charAt(pos);

        // Parênteses
        if (c == '(') {
            pos++;
            Result inside = parseExpression();
            if (pos >= expression.length() || expression.charAt(pos) != ')')
                throw new IllegalArgumentException("Parêntese não fechado");
            pos++;
            return inside;
        }

        // Números (reais ou complexos)
        if (Character.isDigit(c) || c == '.' || c == '+' || c == '-') {
            // Verificar se é um número (pode ser negativo ou positivo)
            if (c == '+' || c == '-') {
                // Verificar se depois do sinal vem um dígito ou ponto
                if (pos + 1 < expression.length() &&
                        (Character.isDigit(expression.charAt(pos + 1)) || expression.charAt(pos + 1) == '.')) {
                    return parseNumber();
                }
            } else {
                return parseNumber();
            }
        }

        // Letras (funções ou variáveis)
        if (Character.isLetter(c)) {
            return parseIdentifier();
        }

        // Raiz quadrada (√)
        if (c == '√') {
            pos++;
            Result r = parsePrimary();

            if (r.value.getImag() != 0)
                throw new IllegalArgumentException("sqrt só suporta números reais.");

            Complex val = Complex.sqrt(r.value.getReal());
            Node node = new Node("√", r.node);
            return new Result(val, node);
        }

        throw new IllegalArgumentException("Caractere inválido: '" + c + "'");
    }

    private Result parseNumber() {
        int start = pos;

        // Verificar se é um número complexo (contém 'i')
        boolean hasImaginary = false;
        for (int i = pos; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == 'i') {
                hasImaginary = true;
                break;
            }
            if (!Character.isDigit(c) && c != '.' && c != '+' && c != '-') {
                break;
            }
        }

        if (hasImaginary) {
            // É um número complexo - parsear até o 'i'
            while (pos < expression.length()) {
                char c = expression.charAt(pos);
                if (c == 'i') {
                    pos++; // Incluir o 'i'
                    break;
                }
                if (!Character.isDigit(c) && c != '.' && c != '+' && c != '-') {
                    break;
                }
                pos++;
            }

            String complexStr = expression.substring(start, pos);
            try {
                Complex val = Complex.parse(complexStr);
                Node node = new Node(complexStr);
                return new Result(val, node);
            } catch (Exception e) {
                throw new IllegalArgumentException("Número complexo inválido: " + complexStr);
            }
        } else {
            // É um número real normal
            // Parte inteira
            while (pos < expression.length() && Character.isDigit(expression.charAt(pos))) {
                pos++;
            }

            // Parte decimal
            if (pos < expression.length() && expression.charAt(pos) == '.') {
                pos++;
                while (pos < expression.length() && Character.isDigit(expression.charAt(pos))) {
                    pos++;
                }
            }

            String numStr = expression.substring(start, pos);
            double value = Double.parseDouble(numStr);
            Node node = new Node(numStr);
            return new Result(new Complex(value, 0), node);
        }
    }

    private Result parseIdentifier() {
        int start = pos;
        while (pos < expression.length() && Character.isLetter(expression.charAt(pos))) {
            pos++;
        }

        String name = expression.substring(start, pos);

        // Verificar se é uma função (tem parênteses logo após)
        if (pos < expression.length() && expression.charAt(pos) == '(') {
            // É uma função
            pos++; // Pular '('
            Result arg = parseExpression();

            if (pos >= expression.length() || expression.charAt(pos) != ')') {
                throw new IllegalArgumentException("Parêntese não fechado na função " + name);
            }
            pos++; // Pular ')'

            Complex val;
            switch (name.toLowerCase()) {
                case "sin":
                    val = Complex.sin(arg.value);
                    break;
                case "cos":
                    val = Complex.cos(arg.value);
                    break;
                case "tan":
                    val = Complex.tan(arg.value);
                    break;
                case "log":
                    val = Complex.log(arg.value);
                    break;
                case "exp":
                    val = Complex.exp(arg.value);
                    break;
                case "abs":
                    val = new Complex(arg.value.abs(), 0);
                    break;
                case "sqrt":
                    if (arg.value.getImag() != 0)
                        throw new IllegalArgumentException("sqrt só suporta números reais.");
                    val = Complex.sqrt(arg.value.getReal());
                    break;
                default:
                    throw new IllegalArgumentException("Função desconhecida: " + name);
            }

            Node node = new Node(name, arg.node);
            return new Result(val, node);
        } else {
            // É uma variável
            if (!allVariables.containsKey(name)) {
                throw new IllegalArgumentException("Variável desconhecida: " + name);
            }

            Complex val = allVariables.get(name);
            Node node = new Node(name);
            return new Result(val, node);
        }
    }
}