import javax.swing.*;
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

    private final String[] botoes = {
            "sin", "cos", "tan", "log", "C",
            "x", "y", "z", "(", ")",
            "7", "8", "9", "/", "*",
            "4", "5", "6", "+", "-",
            "1", "2", "3", "i", ".",
            "^", "√", "0", "=",
    };

    public CalculatorGUI() {
        super("Calculadora de Complexos");
        setTitle("Calculadora de Complexos");
        setSize(500, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        tela = new JTextField();
        tela.setEditable(false);
        tela.setFont(new Font("Arial", Font.BOLD, 32));
        tela.setHorizontalAlignment(SwingConstants.RIGHT);
        tela.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(tela, BorderLayout.NORTH);

        JPanel painel = new JPanel();
        painel.setLayout(new GridLayout(6, 5, 8, 8));
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

            painel.add(botao);
        }

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(painel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private Map<String, Complex> collectVariables(String expression) {
        Set<String> variableNames = new HashSet<>();
        Map<String, Complex> variableMap = new HashMap<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isLetter(c)) {
                String var = String.valueOf(c);

                if (!var.equalsIgnoreCase("i") || (expression.length() == 1 && var.equalsIgnoreCase("i"))) {
                    variableNames.add(var);
                }
            }
        }

        for (String varName : variableNames) {
            String input = JOptionPane.showInputDialog(
                    this,
                    "Digite o valor complexo para a variável " + varName + " (Ex: 3+2i, -5, 1i, i)",
                    "Entrada de Variável",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (input == null || input.trim().isEmpty()) {
                return null;
            }

            try {
                Complex value = Complex.parse(input);
                variableMap.put(varName, value);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Valor complexo inválido para " + varName + ". Tente novamente.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
                return null;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro inesperado ao analisar " + varName + ". Tente novamente.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
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

            Map<String, Complex> variables = collectVariables(textoAtual);
            if (variables == null) {
                tela.setText("Cálculo Cancelado.");
                return;
            }

            try {
                ExpressionParser parser = new ExpressionParser(textoAtual, variables);
                Complex resultado = parser.evaluate();
                tela.setText(resultado.toString());

            } catch (IllegalArgumentException | ArithmeticException ex) {
                tela.setText("Erro: " + ex.getMessage());
                System.err.println("Erro na expressão: " + ex.getMessage());
            } catch (Exception ex) {
                tela.setText("Erro Inesperado");
                ex.printStackTrace();
            }
        } else if (comando.equals("i")) {
            tela.setText(textoAtual + "i");
        } else if (comando.equals("x") || comando.equals("y") || comando.equals("z")) {
            tela.setText(textoAtual + comando);
        } else if (comando.equals("log") || comando.equals("sin") || comando.equals("cos") || comando.equals("tan")) {
            tela.setText(textoAtual + comando + "(");
        }
        else {
            tela.setText(textoAtual + comando);
        }
    }


    public static void main(String[] args){
        SwingUtilities.invokeLater(() ->{
            new CalculatorGUI().setVisible(true);
        });
    }
}