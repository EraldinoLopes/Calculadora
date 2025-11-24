// CalculatorGUI.java
import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class CalculatorGUI extends JFrame {

    private JTextField tela;
    private JTree arvoreExecucao;
    private JTabbedPane abas;

    public CalculatorGUI() {
        super("Calculadora de Complexos - AST");
        setSize(700, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tela = new JTextField();
        tela.setEditable(false);
        tela.setFont(new Font("Arial", Font.BOLD, 28));
        tela.setHorizontalAlignment(SwingConstants.RIGHT);
        add(tela, BorderLayout.NORTH);

        abas = new JTabbedPane();
        abas.add("Calculadora", criarPainelBotoes());
        arvoreExecucao = new JTree(new DefaultMutableTreeNode("Nenhuma expressão avaliada"));
        abas.add("Árvore", new JScrollPane(arvoreExecucao));
        add(abas, BorderLayout.CENTER);
    }

    private JPanel criarPainelBotoes() {
        JPanel painel = new JPanel(new BorderLayout());
        JPanel grid = new JPanel(new GridLayout(6, 5, 6, 6));

        String[] botoes = {
                "sin", "cos", "tan", "log", "C",
                "x", "y", "z", "(", ")",
                "7", "8", "9", "/", "*",
                "4", "5", "6", "+", "-",
                "1", "2", "3", "i", ".",
                "^", "√", "0", "=", "=="
        };

        for (String b : botoes) {
            JButton btn = new JButton(b);
            btn.setFont(new Font("Arial", Font.BOLD, 18));
            btn.addActionListener(new BotaoListener(b));
            grid.add(btn);
        }

        painel.add(grid, BorderLayout.CENTER);
        return painel;
    }

    private class BotaoListener implements ActionListener {
        private final String cmd;
        BotaoListener(String cmd) { this.cmd = cmd; }

        @Override
        public void actionPerformed(ActionEvent e) {
            String texto = tela.getText();

            if (cmd.equals("C")) {
                tela.setText("");
                arvoreExecucao.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Nenhuma expressão avaliada")));
                return;
            }

            if (cmd.equals("=")) {
                if (texto.isEmpty()) return;
                Map<String, Complex> vars = collectVariables(texto);
                if (vars == null) return;
                try {
                    ExpressionParser parser = new ExpressionParser(texto, vars);
                    Complex res = parser.evaluate();
                    tela.setText(res.toString());

                    // Atualizar a árvore com formato LISP
                    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Expressão: " + texto);
                    rootNode.add(parser.getExecutionTree());
                    rootNode.add(new DefaultMutableTreeNode("Resultado: " + res.toString()));
                    rootNode.add(new DefaultMutableTreeNode("Árvore LISP: " + parser.getLispTree()));

                    arvoreExecucao.setModel(new DefaultTreeModel(rootNode));
                    expandAllRows(arvoreExecucao);
                    abas.setSelectedIndex(1);
                } catch (Exception ex) {
                    tela.setText("Erro: " + ex.getMessage());
                }
                return;
            }

            if (cmd.equals("==")) {
                if (texto.isEmpty()) return;
                String expr2 = JOptionPane.showInputDialog(
                        CalculatorGUI.this,
                        "Digite a segunda expressão para comparar com:\n" + texto,
                        "Comparar Expressões",
                        JOptionPane.QUESTION_MESSAGE
                );
                if (expr2 == null || expr2.trim().isEmpty()) {
                    tela.setText("Comparação cancelada.");
                    return;
                }
                Map<String, Complex> vars = collectVariables(texto + expr2);
                if (vars == null) return;
                try {
                    ExpressionParser p1 = new ExpressionParser(texto, vars);
                    ExpressionParser p2 = new ExpressionParser(expr2, vars);
                    p1.evaluate();
                    p2.evaluate();
                    boolean iguais = ExpressionParser.compareAst(p1.getAstRoot(), p2.getAstRoot());

                    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Comparação de Expressões");
                    rootNode.add(new DefaultMutableTreeNode("Expressão 1: " + texto));
                    rootNode.add(new DefaultMutableTreeNode("Expressão 2: " + expr2));
                    rootNode.add(new DefaultMutableTreeNode("São iguais? " + iguais));
                    rootNode.add(new DefaultMutableTreeNode("Árvore LISP 1: " + p1.getLispTree()));
                    rootNode.add(new DefaultMutableTreeNode("Árvore LISP 2: " + p2.getLispTree()));

                    tela.setText(iguais ? "As expressões são estritamente iguais." : "As expressões são diferentes.");
                    arvoreExecucao.setModel(new DefaultTreeModel(rootNode));
                    expandAllRows(arvoreExecucao);
                    abas.setSelectedIndex(1);
                } catch (Exception ex) {
                    tela.setText("Erro: " + ex.getMessage());
                }
                return;
            }

            // funções: inserir "nome(" para o parser reconhecer
            if (cmd.equals("sin") || cmd.equals("cos") || cmd.equals("tan") || cmd.equals("log")) {
                tela.setText(texto + cmd + "(");
                return;
            }

            if (cmd.equals("i")) {
                tela.setText(texto + "i");
                return;
            }

            if (cmd.equals("√")) {
                tela.setText(texto + "√");
                return;
            }

            // default: concatena
            tela.setText(texto + cmd);
        }
    }

    private void expandAllRows(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    /**
     * Extrai nomes de variáveis (sequência de letras/dígitos começando com letra)
     * e pede ao usuário os valores.
     */
    private Map<String, Complex> collectVariables(String expr) {
        Set<String> names = new LinkedHashSet<>();

        // Processar a expressão para identificar variáveis
        // Vamos usar uma abordagem mais simples: procurar por letras isoladas
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (Character.isLetter(c)) {
                // Verificar se é uma letra única (não parte de uma função)
                if (i == 0 || !Character.isLetter(expr.charAt(i - 1))) {
                    // Verificar se não é seguida por '(' (função)
                    if (i + 1 >= expr.length() || expr.charAt(i + 1) != '(') {
                        String potentialVar = String.valueOf(c);
                        // Ignorar "i" que é constante
                        if (!potentialVar.equalsIgnoreCase("i")) {
                            names.add(potentialVar);
                        }
                    }
                }
            }
        }

        Map<String, Complex> vars = new HashMap<>();
        for (String name : names) {
            String input = JOptionPane.showInputDialog(
                    this,
                    "Digite o valor para " + name + " (ex: 3+2i, -1, 4i) ou deixe vazio para 0:",
                    "Entrada de Variável",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (input == null) return null;
            input = input.trim();
            if (input.isEmpty()) input = "0";
            try {
                Complex val = Complex.parse(input);
                vars.put(name, val);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Valor inválido para " + name + ": " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        return vars;
    }

    private boolean isFunction(String token) {
        return token.equalsIgnoreCase("sin") ||
                token.equalsIgnoreCase("cos") ||
                token.equalsIgnoreCase("tan") ||
                token.equalsIgnoreCase("log") ||
                token.equalsIgnoreCase("exp") ||
                token.equalsIgnoreCase("abs") ||
                token.equalsIgnoreCase("sqrt");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculatorGUI().setVisible(true));
    }
}