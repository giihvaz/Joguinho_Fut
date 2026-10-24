

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main extends JPanel implements ActionListener, KeyListener {

    private static final int LARGURA = 800;
    private static final int ALTURA = 600;

    private final Timer timer;
    private final Random random = new Random();

    private final List<Jogador> jogadores = new ArrayList<>();
    private final Disco disco = new Disco();

    private int golsAzuis = 0;
    private int golsVermelhos = 0;

    private boolean cima, baixo, esquerda, direita;
    private boolean ghostHelp = false;
    private boolean fearThePuck = false;

    private long fimPowerUp = 0;
    private long proximoPowerUp = 0;

    private String mensagem = "Use WASD para mover o jogador azul!";

    public Main() {
        setPreferredSize(new Dimension(LARGURA, ALTURA));
        setBackground(new Color(30, 115, 75));
        setFocusable(true);
        addKeyListener(this);

        criarJogadores();
        reiniciarDisco();

        timer = new Timer(16, this);
        timer.start();
    }

    private void criarJogadores() {
        // Equipe azul
        jogadores.add(new Jogador(300, 190, Color.CYAN, true));
        jogadores.add(new Jogador(220, 260, Color.BLUE, true));
        jogadores.add(new Jogador(380, 260, Color.BLUE, true));

        // Equipe vermelha
        jogadores.add(new Jogador(300, 410, Color.PINK, false));
        jogadores.add(new Jogador(220, 340, Color.RED, false));
        jogadores.add(new Jogador(380, 340, Color.RED, false));
    }

    private void reiniciarDisco() {
        disco.x = LARGURA / 2.0;
        disco.y = ALTURA / 2.0;
        disco.vx = random.nextBoolean() ? 3 : -3;
        disco.vy = random.nextBoolean() ? 2 : -2;
        disco.dono = null;

        for (Jogador j : jogadores) {
            j.x = j.azul
                    ? 260 + random.nextInt(200)
                    : 340 + random.nextInt(120);
            j.y = j.azul
                    ? 170 + random.nextInt(100)
                    : 330 + random.nextInt(100);
            j.vx = 0;
            j.vy = 0;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        atualizar();
        repaint();
    }

    private void atualizar() {
        long agora = System.currentTimeMillis();

        if (fimPowerUp > 0 && agora > fimPowerUp) {
            ghostHelp = false;
            fearThePuck = false;
            fimPowerUp = 0;
            mensagem = "Power-up encerrado!";
        }

        if (agora > proximoPowerUp) {
            ativarPowerUp();
            proximoPowerUp = agora + 12000;
        }

        for (Jogador j : jogadores) {
            if (j.azul && j == jogadores.get(0)) {
                moverJogadorPrincipal(j);
            } else {
                moverIA(j);
            }

            limitarJogador(j);
        }

        resolverColisoesJogadores();
        atualizarDisco();
        verificarGol();
    }

    private void moverJogadorPrincipal(Jogador j) {
        double velocidade = ghostHelp ? 4.5 : 3.2;

        j.vx = 0;
        j.vy = 0;

        if (cima) j.vy -= velocidade;
        if (baixo) j.vy += velocidade;
        if (esquerda) j.vx -= velocidade;
        if (direita) j.vx += velocidade;

        j.x += j.vx;
        j.y += j.vy;
    }

    private void moverIA(Jogador j) {
        double alvoX = disco.x;
        double alvoY = disco.y;

        // Quando a equipe tem o disco, os jogadores
        // procuram avançar em direção ao gol adversário.
        if (disco.dono != null) {
            if (disco.dono.azul == j.azul) {
                alvoY = j.azul ? 60 : ALTURA - 60;
                alvoX = disco.x;
            } else {
                alvoX = disco.dono.x;
                alvoY = disco.dono.y;
            }
        }

        double dx = alvoX - j.x;
        double dy = alvoY - j.y;
        double distancia = Math.sqrt(dx * dx + dy * dy);

        // Fear The Puck: a equipe vermelha evita o disco.
        if (fearThePuck && !j.azul && distancia < 150) {
            dx = j.x - disco.x;
            dy = j.y - disco.y;
            distancia = Math.sqrt(dx * dx + dy * dy);
        }

        if (distancia > 1) {
            double velocidade = 2.0;
            j.vx = (dx / distancia) * velocidade;
            j.vy = (dy / distancia) * velocidade;

            j.x += j.vx;
            j.y += j.vy;
        }
    }

    private void limitarJogador(Jogador j) {
        j.x = Math.max(25, Math.min(LARGURA - 25, j.x));
        j.y = Math.max(25, Math.min(ALTURA - 25, j.y));
    }

    private void resolverColisoesJogadores() {
        for (int i = 0; i < jogadores.size(); i++) {
            for (int k = i + 1; k < jogadores.size(); k++) {
                Jogador a = jogadores.get(i);
                Jogador b = jogadores.get(k);

                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double distancia = Math.sqrt(dx * dx + dy * dy);

                if (distancia < 30 && distancia > 0) {
                    double empurrao = (30 - distancia) / 2.0;
                    double nx = dx / distancia;
                    double ny = dy / distancia;

                    a.x -= nx * empurrao;
                    a.y -= ny * empurrao;
                    b.x += nx * empurrao;
                    b.y += ny * empurrao;
                }
            }
        }
    }

    private void atualizarDisco() {
        disco.x += disco.vx;
        disco.y += disco.vy;

        disco.vx *= 0.995;
        disco.vy *= 0.995;

        // Colisão do disco com as laterais da pista.
        if (disco.x < 15 || disco.x > LARGURA - 15) {
            disco.vx *= -1;
            disco.x = Math.max(15, Math.min(LARGURA - 15, disco.x));
        }

        // Colisão do disco com os jogadores.
        for (Jogador j : jogadores) {
            double dx = disco.x - j.x;
            double dy = disco.y - j.y;
            double distancia = Math.sqrt(dx * dx + dy * dy);

            if (distancia < 25) {
                disco.dono = j;

                if (distancia > 0) {
                    disco.vx = (dx / distancia) * 4;
                    disco.vy = (dy / distancia) * 4;
                }

                disco.x = j.x + (dx / Math.max(1, distancia)) * 20;
                disco.y = j.y + (dy / Math.max(1, distancia)) * 20;
            }
        }

        // O disco acompanha seu jogador.
        if (disco.dono != null) {
            Jogador j = disco.dono;
            disco.x = j.x + j.vx * 2;
            disco.y = j.y + j.vy * 2;

            // Jogador controlado pode chutar com ESPAÇO.
            if (j == jogadores.get(0) && chute) {
                disco.vx = direita ? 7 : esquerda ? -7 : 0;
                disco.vy = cima ? -7 : baixo ? 7 : -7;
                disco.dono = null;
                chute = false;
            }
        }
    }

    private boolean chute = false;

    private void verificarGol() {
        int centroGolEsquerda = ALTURA / 2;
        boolean dentroDoGol =
                disco.y > centroGolEsquerda - 55 &&
                        disco.y < centroGolEsquerda + 55;

        // Gols nas extremidades horizontais.
        if (dentroDoGol && disco.x < 10) {
            golsVermelhos++;
            mensagem = "Gol da equipe vermelha!";
            reiniciarDisco();
        } else if (dentroDoGol && disco.x > LARGURA - 10) {
            golsAzuis++;
            mensagem = "Gol da equipe azul!";
            reiniciarDisco();
        } else {
            // Rebater nas extremidades da pista.
            if (disco.y < 10 || disco.y > ALTURA - 10) {
                disco.vy *= -1;
                disco.y = Math.max(10, Math.min(ALTURA - 10, disco.y));
            }
        }
    }

    private void ativarPowerUp() {
        int tipo = random.nextInt(2);

        ghostHelp = tipo == 0;
        fearThePuck = tipo == 1;
        fimPowerUp = System.currentTimeMillis() + 5000;

        mensagem = ghostHelp
                ? "GHOST HELP: jogador azul mais rápido!"
                : "FEAR THE PUCK: vermelhos fogem do disco!";
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        desenharPista(g2);
        desenharJogadores(g2);
        desenharDisco(g2);
        desenharInterface(g2);
    }

    private void desenharPista(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));

        g.drawRect(10, 10, LARGURA - 20, ALTURA - 20);
        g.drawLine(LARGURA / 2, 10, LARGURA / 2, ALTURA - 10);

        g.drawOval(LARGURA / 2 - 60, ALTURA / 2 - 60, 120, 120);

        // Gols nas laterais.
        g.setColor(Color.YELLOW);
        g.fillRect(0, ALTURA / 2 - 55, 15, 110);
        g.fillRect(LARGURA - 15, ALTURA / 2 - 55, 15, 110);
    }

    private void desenharJogadores(Graphics2D g) {
        for (Jogador j : jogadores) {
            g.setColor(j.cor);
            g.fillOval((int) j.x - 12, (int) j.y - 12, 24, 24);

            g.setColor(Color.WHITE);
            g.drawOval((int) j.x - 12, (int) j.y - 12, 24, 24);

            if (disco.dono == j) {
                g.setColor(Color.YELLOW);
                g.drawOval((int) j.x - 17, (int) j.y - 17, 34, 34);
            }
        }
    }

    private void desenharDisco(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillOval((int) disco.x - 7, (int) disco.y - 7, 14, 14);
    }

    private void desenharInterface(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));

        g.drawString(
                "AZUL  " + golsAzuis + "  x  " + golsVermelhos + "  VERMELHO",
                220, 35
        );

        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString(mensagem, 20, ALTURA - 20);
        g.drawString("WASD: mover | ESPAÇO: chutar", 20, 55);
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> cima = true;
            case KeyEvent.VK_S -> baixo = true;
            case KeyEvent.VK_A -> esquerda = true;
            case KeyEvent.VK_D -> direita = true;
            case KeyEvent.VK_SPACE -> chute = true;
        }
    }

    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> cima = false;
            case KeyEvent.VK_S -> baixo = false;
            case KeyEvent.VK_A -> esquerda = false;
            case KeyEvent.VK_D -> direita = false;
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    static class Jogador {
        double x, y, vx, vy;
        Color cor;
        boolean azul;

        Jogador(double x, double y, Color cor, boolean azul) {
            this.x = x;
            this.y = y;
            this.cor = cor;
            this.azul = azul;
        }
    }

    static class Disco {
        double x, y, vx, vy;
        Jogador dono;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame("Hockey AI - Java");
            Main jogo = new Main();

            janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            janela.setResizable(false);
            janela.add(jogo);
            janela.pack();
            janela.setLocationRelativeTo(null);
            janela.setVisible(true);

            jogo.requestFocusInWindow();
        });
    }
}
