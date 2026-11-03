package futebol;

import javax.swing.*;

/** Ponto de entrada: Futebol de Ficha com Steering Behaviors. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame("Futebol de Ficha — Steering Behaviors");
            Tela tela = new Tela();
            janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            janela.setResizable(false);
            janela.add(tela);
            janela.pack();
            janela.setLocationRelativeTo(null);
            janela.setVisible(true);
            tela.requestFocusInWindow();
        });
    }
}
