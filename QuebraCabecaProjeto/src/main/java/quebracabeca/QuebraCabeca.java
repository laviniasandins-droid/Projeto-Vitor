package quebracabeca;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;

/**
 * Quebra-cabeça deslizante (jogo dos números).
 * Objetivo: ordenar as peças de 1 até N, deixando o espaço vazio no canto inferior direito.
 * Basta executar (Shift + F6 no NetBeans).
 */
public class QuebraCabeca extends JFrame {

    private int tamanho = 3;          // 3 = 3x3, 4 = 4x4, 5 = 5x5
    private int[] pecas;              // 0 representa o espaço vazio
    private JButton[] botoes;
    private JPanel painelTabuleiro;
    private JLabel lblMovimentos;
    private JLabel lblTempo;
    private JComboBox<String> comboTamanho;

    private int movimentos;
    private int segundos;
    private boolean jogoIniciado;
    private final Timer cronometro;   // javax.swing.Timer
    private final Random random = new Random();

    public QuebraCabeca() {
        super("Quebra-Cabeça Deslizante");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // ----- Painel superior -----
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));

        comboTamanho = new JComboBox<>(new String[]{"3 x 3", "4 x 4", "5 x 5"});
        comboTamanho.addActionListener(e -> {
            tamanho = comboTamanho.getSelectedIndex() + 3;
            novoJogo();
        });

        JButton btnNovo = new JButton("Novo Jogo");
        btnNovo.addActionListener(e -> novoJogo());

        lblMovimentos = new JLabel("Movimentos: 0");
        lblTempo = new JLabel("Tempo: 0s");

        topo.add(new JLabel("Tamanho:"));
        topo.add(comboTamanho);
        topo.add(btnNovo);
        topo.add(lblMovimentos);
        topo.add(lblTempo);
        add(topo, BorderLayout.NORTH);

        // ----- Tabuleiro -----
        painelTabuleiro = new JPanel();
        painelTabuleiro.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        add(painelTabuleiro, BorderLayout.CENTER);

        // ----- Cronômetro (dispara a cada 1 segundo) -----
        cronometro = new Timer(1000, (ActionEvent e) -> {
            segundos++;
            lblTempo.setText("Tempo: " + segundos + "s");
        });

        setSize(450, 520);
        setLocationRelativeTo(null);
        novoJogo();
    }

    /** Reinicia o jogo: monta o tabuleiro e embaralha. */
    private void novoJogo() {
        cronometro.stop();
        jogoIniciado = false;
        movimentos = 0;
        segundos = 0;
        lblMovimentos.setText("Movimentos: 0");
        lblTempo.setText("Tempo: 0s");

        int total = tamanho * tamanho;
        pecas = new int[total];
        for (int i = 0; i < total - 1; i++) {
            pecas[i] = i + 1;
        }
        pecas[total - 1] = 0;

        embaralhar();
        montarTabuleiro();
    }

    /**
     * Embaralha fazendo movimentos válidos aleatórios a partir do estado resolvido.
     * Assim, o quebra-cabeça SEMPRE tem solução.
     */
    private void embaralhar() {
        int vazio = pecas.length - 1;
        int anterior = -1;
        int passos = tamanho * tamanho * 40;

        for (int k = 0; k < passos; k++) {
            int[] vizinhos = vizinhosDe(vazio);
            int escolhido;
            do {
                escolhido = vizinhos[random.nextInt(vizinhos.length)];
            } while (escolhido == anterior && vizinhos.length > 1);

            pecas[vazio] = pecas[escolhido];
            pecas[escolhido] = 0;
            anterior = vazio;
            vazio = escolhido;
        }

        // Se por acaso ficou resolvido, embaralha de novo
        if (venceu()) {
            embaralhar();
        }
    }

    /** Retorna os índices das casas vizinhas (cima, baixo, esquerda, direita). */
    private int[] vizinhosDe(int indice) {
        int linha = indice / tamanho;
        int coluna = indice % tamanho;
        int[] temp = new int[4];
        int n = 0;
        if (linha > 0)           temp[n++] = indice - tamanho;
        if (linha < tamanho - 1) temp[n++] = indice + tamanho;
        if (coluna > 0)          temp[n++] = indice - 1;
        if (coluna < tamanho - 1) temp[n++] = indice + 1;
        int[] resultado = new int[n];
        System.arraycopy(temp, 0, resultado, 0, n);
        return resultado;
    }

    /** Cria os botões do tabuleiro. */
    private void montarTabuleiro() {
        painelTabuleiro.removeAll();
        painelTabuleiro.setLayout(new GridLayout(tamanho, tamanho, 4, 4));

        botoes = new JButton[tamanho * tamanho];
        int fonte = tamanho == 3 ? 40 : (tamanho == 4 ? 30 : 24);

        for (int i = 0; i < botoes.length; i++) {
            final int indice = i;
            JButton b = new JButton();
            b.setFont(new Font("SansSerif", Font.BOLD, fonte));
            b.setFocusPainted(false);
            b.addActionListener(e -> clicar(indice));
            botoes[i] = b;
            painelTabuleiro.add(b);
        }

        atualizarTabuleiro();
        painelTabuleiro.revalidate();
        painelTabuleiro.repaint();
    }

    /** Atualiza texto e cor de cada botão conforme o estado do jogo. */
    private void atualizarTabuleiro() {
        for (int i = 0; i < pecas.length; i++) {
            JButton b = botoes[i];
            if (pecas[i] == 0) {
                b.setText("");
                b.setEnabled(false);
                b.setBackground(new Color(220, 220, 220));
            } else {
                b.setText(String.valueOf(pecas[i]));
                b.setEnabled(true);
                // Peça na posição correta fica verde
                boolean correta = pecas[i] == i + 1;
                b.setBackground(correta ? new Color(150, 220, 150) : new Color(135, 190, 245));
                b.setOpaque(true);
            }
        }
    }

    /** Trata o clique em uma peça. */
    private void clicar(int indice) {
        int vazio = indiceDoVazio();

        // Só move se a peça clicada for vizinha do espaço vazio
        boolean vizinha = false;
        for (int v : vizinhosDe(vazio)) {
            if (v == indice) {
                vizinha = true;
                break;
            }
        }
        if (!vizinha) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!jogoIniciado) {
            jogoIniciado = true;
            cronometro.start();
        }

        pecas[vazio] = pecas[indice];
        pecas[indice] = 0;
        movimentos++;
        lblMovimentos.setText("Movimentos: " + movimentos);
        atualizarTabuleiro();

        if (venceu()) {
            cronometro.stop();
            JOptionPane.showMessageDialog(this,
                    "Parabéns, você venceu!\n\nMovimentos: " + movimentos
                            + "\nTempo: " + segundos + " segundos",
                    "Vitória!", JOptionPane.INFORMATION_MESSAGE);
            novoJogo();
        }
    }

    private int indiceDoVazio() {
        for (int i = 0; i < pecas.length; i++) {
            if (pecas[i] == 0) return i;
        }
        return -1;
    }

    /** Verifica se as peças estão em ordem: 1, 2, 3 ... N-1, vazio. */
    private boolean venceu() {
        for (int i = 0; i < pecas.length - 1; i++) {
            if (pecas[i] != i + 1) return false;
        }
        return pecas[pecas.length - 1] == 0;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuebraCabeca().setVisible(true));
    }
}
