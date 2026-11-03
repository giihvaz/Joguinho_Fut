package futebol;

import futebol.motor.Vetor;
import futebol.selecoes.Selecao;
import futebol.som.Som;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.Random;

/**
 * Roleta que sorteia o adversário do Brasil entre as 47 seleções classificadas
 * para a Copa de 2026. A roda gira com desaceleração (ease-out) e o ponteiro,
 * no topo, indica a seleção sorteada.
 */
public final class Roleta {

    public static final double DURACAO = 5.5;   // s
    private static final double VOLTAS = 5;

    private final List<Selecao> selecoes = Selecao.ADVERSARIOS;
    private final int n = selecoes.size();
    private final double passo = 2 * Math.PI / n;
    private final Random rnd = new Random();
    private final Som som;

    // Geometria (coordenadas de tela)
    private static final double CX = 410, CY = 392, RAIO = 290;

    private double rotacao = 0;
    private double inicio, fim, tempo;
    private boolean girando = false;
    private int sorteada = -1;
    private int ultimoIndice = -1;
    private double brilho = 0;

    public final Botao botaoComecar = new Botao("Começar partida", 760, 470, 280, 58, new Color(0, 140, 60));
    public final Botao botaoGirar = new Botao("Girar de novo", 760, 545, 280, 50, new Color(60, 70, 90));

    public Roleta(Som som) {
        this.som = som;
    }

    public void girar() {
        int alvo = rnd.nextInt(n);
        // Rotação (mod 2π) que coloca o alvo sob o ponteiro, com um pequeno desvio dentro da fatia.
        double desejada = mod(-alvo * passo + (rnd.nextDouble() - 0.5) * passo * 0.6);
        double delta = mod(desejada - mod(rotacao));
        inicio = rotacao;
        fim = rotacao + VOLTAS * 2 * Math.PI + delta;
        tempo = 0;
        girando = true;
        sorteada = alvo;
        brilho = 0;
    }

    /** Pula a animação (clique durante o giro). */
    public void pular() {
        if (!girando) return;
        tempo = DURACAO;
    }

    public boolean terminou() { return !girando && sorteada >= 0; }

    public Selecao resultado() { return sorteada < 0 ? null : selecoes.get(sorteada); }

    public void atualizar(double dt) {
        if (girando) {
            tempo = Math.min(DURACAO, tempo + dt);
            double u = tempo / DURACAO;
            double ease = 1 - Math.pow(1 - u, 3);    // desacelera no final
            rotacao = inicio + (fim - inicio) * ease;

            int idx = indiceNoPonteiro();
            if (idx != ultimoIndice) {
                ultimoIndice = idx;
                som.tocar(Som.Efeito.TIQUE, 0.35);
            }
            if (tempo >= DURACAO) {
                girando = false;
                som.tocar(Som.Efeito.REVELACAO, 0.8);
            }
        } else if (sorteada >= 0) {
            brilho += dt;
        }
    }

    private int indiceNoPonteiro() {
        return Math.floorMod((int) Math.round(-rotacao / passo), n);
    }

    private static double mod(double a) {
        double m = a % (2 * Math.PI);
        return m < 0 ? m + 2 * Math.PI : m;
    }

    // ------------------------------ DESENHO ------------------------------

    public void desenhar(Graphics2D g, int largura, int altura) {
        g.setPaint(new GradientPaint(0, 0, new Color(18, 40, 28), 0, altura, new Color(10, 20, 16)));
        g.fillRect(0, 0, largura, altura);

        // Título
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.drawString("SORTEIO DO ADVERSÁRIO", 760, 80);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(190, 210, 195));
        g.drawString("Copa do Mundo 2026 — 47 seleções possíveis", 760, 104);

        desenharRoda(g);
        desenharPainel(g);
    }

    private void desenharRoda(Graphics2D g) {
        int atual = indiceNoPonteiro();

        // Aro externo (como a moldura da mesa)
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(CX - RAIO - 18 + 6, CY - RAIO - 18 + 8, 2 * RAIO + 36, 2 * RAIO + 36));
        g.setColor(new Color(150, 30, 35));
        g.fill(new Ellipse2D.Double(CX - RAIO - 18, CY - RAIO - 18, 2 * RAIO + 36, 2 * RAIO + 36));
        // Luzinhas no aro
        for (int i = 0; i < 36; i++) {
            double a = i * 2 * Math.PI / 36 + (girando ? tempo * 2 : brilho);
            boolean acesa = (i + (int) ((girando ? tempo : brilho) * 8)) % 2 == 0;
            g.setColor(acesa ? new Color(255, 230, 120) : new Color(120, 60, 40));
            double x = CX + Math.cos(a) * (RAIO + 9), y = CY + Math.sin(a) * (RAIO + 9);
            g.fill(new Ellipse2D.Double(x - 3.5, y - 3.5, 7, 7));
        }

        // Fatias
        for (int i = 0; i < n; i++) {
            double centro = rotacao + i * passo - Math.PI / 2;
            double graus = Math.toDegrees(centro - passo / 2);
            Arc2D fatia = new Arc2D.Double(CX - RAIO, CY - RAIO, 2 * RAIO, 2 * RAIO,
                    -graus - Math.toDegrees(passo), Math.toDegrees(passo), Arc2D.PIE);
            Color cor = i % 2 == 0 ? new Color(58, 140, 60) : new Color(44, 116, 48);
            if (i == atual && sorteada >= 0 && !girando) {
                float pulso = (float) (0.5 + 0.5 * Math.sin(brilho * 6));
                cor = blend(new Color(240, 200, 40), cor, 0.55f + 0.35f * pulso);
            }
            g.setColor(cor);
            g.fill(fatia);
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(1f));
            g.draw(fatia);
        }

        // Fichas com as bandeiras
        for (int i = 0; i < n; i++) {
            double ang = rotacao + i * passo - Math.PI / 2;
            Vetor p = Vetor.de(CX + Math.cos(ang) * (RAIO - 24), CY + Math.sin(ang) * (RAIO - 24));
            double r = i == atual ? 17 : 14.5;
            Desenho.ficha(g, p, r, selecoes.get(i), false, false);
        }

        // Miolo
        double rm = RAIO - 58;
        g.setColor(new Color(22, 26, 32));
        g.fill(new Ellipse2D.Double(CX - rm, CY - rm, 2 * rm, 2 * rm));
        g.setColor(new Color(255, 255, 255, 40));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(CX - rm, CY - rm, 2 * rm, 2 * rm));

        Selecao s = selecoes.get(atual);
        Desenho.ficha(g, Vetor.de(CX - 70, CY - 40), 40, Selecao.BRASIL, false, false);
        Desenho.ficha(g, Vetor.de(CX + 70, CY - 40), 40, s, false, false);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        centralizar(g, "x", CX, CY - 31);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        centralizar(g, s.nome().toUpperCase(), CX, CY + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(180, 190, 200));
        centralizar(g, s.sigla() + " · " + s.confederacao(), CX, CY + 70);

        // Ponteiro
        Path2D seta = new Path2D.Double();
        seta.moveTo(CX - 16, CY - RAIO - 30);
        seta.lineTo(CX + 16, CY - RAIO - 30);
        seta.lineTo(CX, CY - RAIO + 6);
        seta.closePath();
        g.setColor(new Color(0, 0, 0, 100));
        g.fill(AffineTransform.getTranslateInstance(2, 3).createTransformedShape(seta));
        g.setColor(new Color(255, 215, 0));
        g.fill(seta);
        g.setColor(new Color(120, 90, 0));
        g.setStroke(new BasicStroke(2f));
        g.draw(seta);
    }

    private void desenharPainel(Graphics2D g) {
        if (girando || sorteada < 0) {
            g.setColor(new Color(220, 230, 225));
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString("Girando...", 760, 200);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(160, 175, 170));
            g.drawString("(clique para parar de uma vez)", 760, 224);
            return;
        }
        Selecao s = resultado();
        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("ADVERSÁRIO SORTEADO", 760, 175);
        Desenho.ficha(g, Vetor.de(810, 250), 44, s, false, false);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, s.nome().length() > 14 ? 20 : 28));
        g.drawString(s.nome(), 870, 245);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(190, 205, 200));
        g.drawString(s.sigla() + " · " + s.confederacao(), 870, 268);

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(220, 230, 225));
        g.drawString("Brasil x " + s.nome(), 760, 340);
        g.drawString("Você ataca para a direita →", 760, 364);

        botaoComecar.desenhar(g);
        botaoGirar.desenhar(g);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(150, 165, 160));
        g.drawString("Enter: começar   Espaço: girar de novo   Esc: menu", 760, 625);
    }

    private static void centralizar(Graphics2D g, String t, double cx, double y) {
        g.drawString(t, (float) (cx - g.getFontMetrics().stringWidth(t) / 2.0), (float) y);
    }

    private static Color blend(Color a, Color b, float t) {
        return new Color(
                (int) (a.getRed() * t + b.getRed() * (1 - t)),
                (int) (a.getGreen() * t + b.getGreen() * (1 - t)),
                (int) (a.getBlue() * t + b.getBlue() * (1 - t)));
    }
}
