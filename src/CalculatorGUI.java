import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class CalculatorGUI extends JFrame implements ActionListener {

    private JTextField tela;
    private JTree arvoreExecucao;
    private JScrollPane scrollArvore;
    private JPanel painelPrincipal;
    private JTabbedPane abas;

    private final String[] botoes = {
            "sin", "cos", "tan", "log", "C",
            "x", "y", "z", "(", ")",
            "7", "8", "9", "/", "*",
            "4", "5", "6", "+", "-",
            "1", "2", "3", "i", ".",
            "^", "√", "0", "=", "==",
    };

    public CalculatorGUI() {
        super("Calculadora de Complexos");
        setSize(600, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        abas = new JTabbedPane();

        painelPrincipal = criarPainelCalculadora();
        abas.add("Calculadora", painelPrincipal);

        arvoreExecucao = new JTree(new DefaultMutableTreeNode("Nenhuma expressão avaliada"));
        scrollArvore = new JScrollPane(arvoreExecucao);
        abas.add("Árvore", scrollArvore);

        add(abas);
    }

    private JPanel criarPainelCalculadora() {
        JPanel painel = new JPanel(new BorderLayout(5, 5));

        tela = new JTextField();
        tela.setEditable(false);
        tela.setFont(new Font("Arial", Font.BOLD, 32));
        tela.setHorizontalAlignment(SwingConstants.RIGHT);
        tela.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        painel.add(tela, BorderLayout.NORTH);

        JPanel painelBotoes = new JPanel();
        painelBotoes.setLayout(new GridLayout(6, 5, 8, 8));

        for (String texto : botoes) {
            JButton botao = new JButton(texto);
            botao.setFont(new Font("Arial", Font.BOLD, 20));
            botao.addActionListener(this);

            if (Objects.equals(texto, "=")) {
                botao.setBackground(new Color(0, 128, 255));
                botao.setForeground(Color.WHITE);
            } else if (texto.length() > 1 || (!Character.isDigit(texto.charAt(0)) && !Objects.equals(texto, "."))) {
                botao.setBackground(new Color(200, 200, 200));
            }
            painelBotoes.add(botao);
        }

        painel.add(painelBotoes, BorderLayout.CENTER);
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return painel;
    }

    private Map<String, Complex> collectVariables(String expression) {
        Set<String> variableNames = new HashSet<>();
        Map<String, Complex> variableMap = new HashMap<>();

        for (char c : expression.toCharArray()) {
            if (Character.isLetter(c)) {
                String var = String.valueOf(c);
                if (!var.equalsIgnoreCase("i")) {
                    variableNames.add(var);
                }
            }
        }

        for (String varName : variableNames) {
            String input = JOptionPane.showInputDialog(
                    this,
                    "Digite o valor para " + varName + " (ex: 3+2i, -1, 4i)",
                    "Entrada de Variável",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (input == null) return null;

            try {
                Complex value = Complex.parse(input);
                variableMap.put(varName, value);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Valor inválido para " + varName,
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        return variableMap;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String comando = e.getActionCommand();
        String textoAtual = tela.getText();

        if (comando.equals("C")) {
            tela.setText("");
        } else if (comando.equals("=")) {
            if (textoAtual.isEmpty()) return;

            Map<String, Complex> vars = collectVariables(textoAtual);
            if (vars == null) return;

            try {
                ExpressionParser parser = new ExpressionParser(textoAtual, vars);
                Complex resultado = parser.evaluate();
                tela.setText(resultado.toString());

                DefaultMutableTreeNode raiz = parser.getExecutionTree();
                arvoreExecucao.setModel(new DefaultTreeModel(raiz));

                for (int i = 0; i < arvoreExecucao.getRowCount(); i++) {
                    arvoreExecucao.expandRow(i);
                }

                abas.setSelectedIndex(1);

            } catch (Exception ex) {
                tela.setText("Erro");
                ex.printStackTrace();
            }

        } else if (comando.equals("i")) {
            tela.setText(textoAtual + "i");
        } else if ("xyz".contains(comando)) {
            tela.setText(textoAtual + comando);
        } else if ("log sin cos tan".contains(comando)) {
            tela.setText(textoAtual + comando + "(");
        } else if (comando.equals("==")) {
            if (textoAtual.isEmpty()) return;

            String expressao1 = textoAtual;
            String expressao2 = JOptionPane.showInputDialog(
                    this,
                    "Digite a segunda expressão para comparar com:\n" + expressao1,
                    "Comparar Expressões",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (expressao2 == null || expressao2.trim().isEmpty()) {
                tela.setText("Comparação cancelada.");
                return;
            }

            // Reutilizar o método collectVariables para resolver variáveis nas duas expressões
            // (coletar apenas uma vez, mas usá-las nas duas expressões)
            Map<String, Complex> variables = collectVariables(expressao1 + expressao2); // Para capturar todas as variáveis usadas
            if (variables == null) {
                tela.setText("Comparação cancelada.");
                return;
            }

            try {
                ExpressionParser parser1 = new ExpressionParser(expressao1, variables);
                ExpressionParser parser2 = new ExpressionParser(expressao2, variables);

                Complex resultado1 = parser1.evaluate();
                Complex resultado2 = parser2.evaluate();

                if (resultado1.equals(resultado2)) {
                    tela.setText("As expressões são iguais!");
                } else {
                    tela.setText("As expressões são diferentes.");
                }
            } catch (Exception ex) {
                tela.setText("Erro: " + ex.getMessage());
            }
        }
        else {
            tela.setText(textoAtual + comando);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculatorGUI().setVisible(true));
    }
}
