package futebol;

import futebol.jogo.Campo;
import futebol.jogo.Ficha;
import futebol.jogo.Partida;
import futebol.jogo.Time;
import futebol.lab.Laboratorio;
import futebol.motor.Obstaculo;
import futebol.motor.Vetor;
import futebol.selecoes.Selecao;
import futebol.som.Som;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Painel principal: laço de jogo, entrada (mouse/teclado), HUD e troca de cenas:
 * MENU → ROLETA (sorteio do adversário) → PARTIDA (com o Laboratório na tecla L).
 */
public class Tela extends JPanel {

    public static final int OX = 40;    // margem do gramado na tela
    public static final int OY = 74;
    public static final int LARGURA = (int) Campo.LARGURA + 2 * OX;
    public static final int ALTURA = (int) Campo.ALTURA + OY + 66;

    /** Passo fixo da física (subdivide quadros lentos para colisões estáveis). */
    private static final double PASSO = 1.0 / 120;

    public enum Cena { MENU, ROLETA, PARTIDA }

    private final Som som = new Som();
    private final Menu menu = new Menu(LARGURA);
    private final Roleta roleta = new Roleta(som);
    private Partida partida;
    private final Laboratorio lab = new Laboratorio();

    private Cena cena = Cena.MENU;

    private boolean modoLab = false;
    private boolean debug = true;
    private boolean rotulos = true;
    private boolean pausado = false;
    private long ultimoNs = System.nanoTime();

    public Tela() {
        setPreferredSize(new Dimension(LARGURA, ALTURA));
        setBackground(new Color(28, 32, 38));
        setFocusable(true);
        configurarEntrada();
        new Timer(15, e -> quadro()).start();
    }

    private void quadro() {
        long agora = System.nanoTime();
        double dt = Math.min((agora - ultimoNs) / 1e9, 0.05);
        ultimoNs = agora;
        if (!pausado || cena != Cena.PARTIDA) avancar(dt);
        repaint();
    }

    /** Avança a simulação em passos fixos (também usado pelos testes). */
    public void avancar(double dt) {
        switch (cena) {
            case MENU -> menu.atualizar(dt);
            case ROLETA -> roleta.atualizar(dt);
            case PARTIDA -> {
                int passos = Math.max(1, (int) Math.ceil(dt / PASSO));
                double h = dt / passos;
                for (int i = 0; i < passos; i++) {
                    if (modoLab) lab.atualizar(h);
                    else partida.atualizar(h);
                }
            }
        }
    }

    // ------------------------------ CENAS ------------------------------

    public void irParaMenu() {
        cena = Cena.MENU;
        som.pararTorcida();
    }

    public void irParaRoleta() {
        cena = Cena.ROLETA;
        som.pararTorcida();
        roleta.girar();
    }

    public void comecarPartida(Selecao adversario) {
        partida = new Partida(adversario);
        partida.ouvinte = new Partida.Ouvinte() {
            @Override public void bolaTocada(double impulso) {
                if (impulso > 8) som.tocar(Som.Efeito.BOLA, 0.25 + impulso / 160);
            }
            @Override public void fichasChocaram(double impulso) {
                if (impulso > 25) som.tocar(Som.Efeito.FICHAS, 0.2 + impulso / 400);
            }
            @Override public void gol(Time quemFez) {
                som.tocar(Som.Efeito.GOL, 1.0);
            }
            @Override public void peteleco(double velocidade, Time time) {
                // O seu peteleco soa mais alto que os da IA.
                double base = time == Time.BRASIL && !modoLab ? 0.45 : 0.25;
                som.tocar(Som.Efeito.PETELECO, base + 0.55 * velocidade / Partida.FORCA_MAX_PETELECO);
            }
        };
        partida.mostrar("Brasil x " + adversario.nome() + "! Arraste uma ficha do Brasil para trás e solte para dar o peteleco.");
        partida.tempoMensagem = 6;
        modoLab = false;
        pausado = false;
        cena = Cena.PARTIDA;
        som.iniciarTorcida();
    }

    private void alternarSom() {
        som.alternar();
        if (partida != null && cena == Cena.PARTIDA) {
            partida.mostrar(!som.isDisponivel() ? "Nenhuma saída de áudio encontrada neste computador."
                    : som.isLigado() ? "Som LIGADO" : "Som DESLIGADO");
        }
    }

    // ------------------------------ ENTRADA ------------------------------

    private static Vetor mundo(MouseEvent e) {
        return Vetor.de(e.getX() - OX, e.getY() - OY);
    }

    private void configurarEntrada() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                switch (cena) {
                    case MENU -> {
                        if (menu.jogar.contem(e.getX(), e.getY())) irParaRoleta();
                        else if (menu.sair.contem(e.getX(), e.getY())) sair();
                    }
                    case ROLETA -> {
                        if (!roleta.terminou()) roleta.pular();
                        else if (roleta.botaoComecar.contem(e.getX(), e.getY())) comecarPartida(roleta.resultado());
                        else if (roleta.botaoGirar.contem(e.getX(), e.getY())) roleta.girar();
                    }
                    case PARTIDA -> {
                        if (modoLab) lab.clique = mundo(e);
                        else partida.pressionar(mundo(e));
                    }
                }
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (cena != Cena.PARTIDA) return;
                if (modoLab) lab.mouse = mundo(e);
                else partida.arrastar(mundo(e));
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (cena == Cena.PARTIDA && !modoLab) partida.soltar(mundo(e));
            }
            @Override public void mouseMoved(MouseEvent e) {
                int x = e.getX(), y = e.getY();
                menu.jogar.sobre = menu.jogar.contem(x, y);
                menu.sair.sobre = menu.sair.contem(x, y);
                roleta.botaoComecar.sobre = roleta.botaoComecar.contem(x, y);
                roleta.botaoGirar.sobre = roleta.botaoGirar.contem(x, y);
                if (cena == Cena.PARTIDA && modoLab) lab.mouse = mundo(e);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_M) {
                    alternarSom();
                    return;
                }
                if (cena == Cena.MENU) {
                    if (k == KeyEvent.VK_ENTER) irParaRoleta();
                    else if (k == KeyEvent.VK_ESCAPE) sair();
                    return;
                }
                if (cena == Cena.ROLETA) {
                    if (k == KeyEvent.VK_ESCAPE) irParaMenu();
                    else if (!roleta.terminou()) { if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER) roleta.pular(); }
                    else if (k == KeyEvent.VK_ENTER) comecarPartida(roleta.resultado());
                    else if (k == KeyEvent.VK_SPACE) roleta.girar();
                    return;
                }
                if (k == KeyEvent.VK_ESCAPE) {
                    irParaMenu();
                    return;
                }
                if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_8) {
                    modoLab = true;
                    lab.trocarModo(k - KeyEvent.VK_0);
                    return;
                }
                switch (k) {
                    case KeyEvent.VK_L -> modoLab = !modoLab;
                    case KeyEvent.VK_D -> debug = !debug;
                    case KeyEvent.VK_E -> rotulos = !rotulos;
                    case KeyEvent.VK_P, KeyEvent.VK_SPACE -> pausado = !pausado;
                    case KeyEvent.VK_R -> { if (modoLab) lab.trocarModo(lab.modo); else partida.reiniciar(); }
                    case KeyEvent.VK_Z -> partida.zerarPlacar();
                    case KeyEvent.VK_I -> {
                        partida.iaBrasil = !partida.iaBrasil;
                        partida.mostrar(partida.iaBrasil
                                ? "IA do Brasil LIGADA: suas fichas jogam sozinhas (e você ainda pode dar petelecos)."
                                : "IA do Brasil DESLIGADA: suas fichas só se mexem com peteleco.");
                    }
                    default -> { }
                }
            }
        });
    }

    // ------------------------------ DESENHO ------------------------------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        desenhar(g);
        g.dispose();
    }

    /** Desenha a tela inteira (público para gerar capturas nos testes). */
    public void desenhar(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(getBackground());
        g.fillRect(0, 0, LARGURA, ALTURA);

        if (cena == Cena.MENU) {
            menu.desenhar(g, LARGURA, ALTURA, som.isLigado());
            return;
        }
        if (cena == Cena.ROLETA) {
            roleta.desenhar(g, LARGURA, ALTURA);
            indicadorSom(g);
            return;
        }

        Graphics2D mundo = (Graphics2D) g.create();
        mundo.translate(OX, OY);
        if (modoLab) desenharLab(mundo); else desenharPartida(mundo);
        mundo.dispose();

        if (modoLab) hudLab(g); else hudPartida(g);
        indicadorSom(g);
        if (pausado) faixa(g, "PAUSADO (P para continuar)", new Color(0, 0, 0, 160));
    }

    private void desenharPartida(Graphics2D g) {
        Desenho.campo(g, true);
        if (debug) {
            Desenho.caminho(g, partida.tatica.caminhoGoleiro(Time.BRASIL), new Color(255, 160, 255));
            Desenho.caminho(g, partida.tatica.caminhoGoleiro(Time.ADVERSARIO), new Color(255, 160, 255));
        }
        for (Ficha f : partida.fichas) {
            boolean destaque = f == partida.selecionada || f == partida.ativo(f.time);
            Desenho.ficha(g, f, destaque);
        }
        Desenho.bola(g, partida.bola);
        if (debug) for (Ficha f : partida.fichas) Desenho.debugAgente(g, f, rotulos);

        if (partida.selecionada != null && partida.pontoArraste != null) {
            Ficha f = partida.selecionada;
            Vetor v = partida.velocidadePeteleco(f, partida.pontoArraste);
            Desenho.mira(g, f.pos, v, v.tamanho() / Partida.FORCA_MAX_PETELECO, partida.pontoArraste);
        }
    }

    private void desenharLab(Graphics2D g) {
        Desenho.campo(g, false);
        if (lab.usaCaminho() || debug) {
            Desenho.caminho(g, lab.caminho, lab.usaCaminho() ? new Color(255, 160, 255) : new Color(255, 255, 255, 90));
        }
        for (Obstaculo o : lab.cones) Desenho.cone(g, o);

        // Alvo do mouse / clique
        g.setColor(new Color(255, 255, 255, 160));
        if (lab.modo == 3 && lab.clique != null) {
            Desenho.circulo(g, lab.clique, 6, true);
        } else if (lab.modo != 3 && lab.modo != 4 && lab.modo != 6 && lab.modo != 7) {
            Desenho.circulo(g, lab.mouse, 7, false);
        }

        Desenho.ficha(g, lab.ficha, false);
        if (debug) Desenho.debugAgente(g, lab.ficha, false);
    }

    // ------------------------------ HUD ------------------------------

    private void hudPartida(Graphics2D g) {
        // Placar
        int cx = LARGURA / 2;
        g.setColor(new Color(15, 18, 22));
        g.fill(new RoundRectangle2D.Double(cx - 170, 12, 340, 44, 14, 14));
        Desenho.ficha(g, Vetor.de(cx - 140, 34), 14, Selecao.BRASIL, false, false);
        Desenho.ficha(g, Vetor.de(cx + 140, 34), 14, partida.adversario, false, false);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        String placar = "BRA  " + partida.golsBrasil + "  x  " + partida.golsAdversario + "  " + partida.adversario.sigla();
        int w = g.getFontMetrics().stringWidth(placar);
        g.drawString(placar, cx - w / 2, 42);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(200, 205, 210));
        g.drawString("IA do Brasil: " + (partida.iaBrasil ? "ligada" : "desligada"), 18, 30);
        g.drawString("Debug: " + (debug ? "ligado" : "desligado"), 18, 48);

        int base = ALTURA - 38;
        g.drawString("Arraste uma ficha do Brasil para TRÁS e solte = peteleco    |    L / 1-8: laboratório de comportamentos",
                18, base);
        g.drawString("D: debug   E: rótulos   I: IA do Brasil   R: reposicionar   Z: zerar placar   P: pausa   M: som   Esc: menu",
                18, base + 18);

        if (partida.tempoMensagem > 0) {
            boolean gol = partida.pausaGol > 0;
            if (gol) faixa(g, partida.mensagem, new Color(0, 90, 30, 200));
            else {
                g.setFont(new Font("SansSerif", Font.BOLD, 13));
                int mw = g.getFontMetrics().stringWidth(partida.mensagem);
                g.setColor(new Color(0, 0, 0, 150));
                g.fill(new RoundRectangle2D.Double(cx - mw / 2.0 - 10, OY + 8, mw + 20, 24, 10, 10));
                g.setColor(Color.WHITE);
                g.drawString(partida.mensagem, cx - mw / 2, OY + 25);
            }
        }
    }

    private void hudLab(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("LABORATÓRIO  —  " + Laboratorio.MODOS[lab.modo].substring(3), 18, 32);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(255, 210, 120));
        g.drawString("Pesos (em ordem de prioridade): " + Laboratorio.PESOS[lab.modo], 18, 54);

        g.setColor(new Color(200, 205, 210));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int base = ALTURA - 38;
        g.drawString("1 Seek   2 Flee   3 Arrive   4 Wander   5 Obstacle Avoidance   6 Path Following   7 Path+Desvio   8 Wander+Desvio+Flee",
                18, base);
        g.drawString("Mouse = alvo/ameaça   Clique = destino do Arrive   D: debug   R: reiniciar ficha   L: voltar para a partida   P: pausa   Esc: menu",
                18, base + 18);
    }

    private void faixa(Graphics2D g, String texto, Color fundo) {
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        int w = g.getFontMetrics().stringWidth(texto);
        int y = OY + (int) Campo.ALTURA / 2;
        g.setColor(fundo);
        g.fillRect(OX, y - 40, (int) Campo.LARGURA, 64);
        g.setColor(Color.WHITE);
        g.drawString(texto, LARGURA / 2 - w / 2, y + 4);
    }

    private void indicadorSom(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(200, 205, 210));
        String t = "Som: " + (!som.isDisponivel() ? "indisponível" : som.isLigado() ? "ligado" : "desligado") + " (M)";
        g.drawString(t, LARGURA - g.getFontMetrics().stringWidth(t) - 18, cena == Cena.ROLETA ? ALTURA - 20 : 30);
    }

    private void sair() {
        System.exit(0);
    }

    // Acesso para testes
    public Cena cena() { return cena; }
    public Roleta roleta() { return roleta; }
    public Partida partida() { return partida; }
    public Laboratorio lab() { return lab; }
    public void setModoLab(boolean v) { modoLab = v; }
}
