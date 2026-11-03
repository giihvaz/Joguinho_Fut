package futebol;

import futebol.jogo.Bola;
import futebol.jogo.Campo;
import futebol.jogo.Ficha;
import futebol.jogo.Papel;
import futebol.jogo.Time;
import futebol.motor.Agente;
import futebol.motor.Caminho;
import futebol.motor.Obstaculo;
import futebol.motor.Vetor;
import futebol.selecoes.Selecao;

import java.awt.*;
import java.awt.geom.*;

/** Toda a parte visual (Java2D). Coordenadas já em "mundo" (origem no canto do gramado). */
public final class Desenho {
    private Desenho() { }

    static final Color GRAMA_1 = new Color(58, 140, 60);
    static final Color GRAMA_2 = new Color(66, 152, 66);
    static final Color LINHA = new Color(255, 255, 255, 215);
    static final Color MOLDURA = new Color(150, 30, 35);

    // ------------------------------ CAMPO ------------------------------

    public static void campo(Graphics2D g, boolean comMarcacoes) {
        double L = Campo.LARGURA, A = Campo.ALTURA;

        // Moldura (a "mesa")
        g.setColor(MOLDURA);
        g.fill(new RoundRectangle2D.Double(-14, -14, L + 28, A + 28, 18, 18));

        // Faixas de grama
        int faixas = 12;
        for (int i = 0; i < faixas; i++) {
            g.setColor(i % 2 == 0 ? GRAMA_1 : GRAMA_2);
            g.fill(new Rectangle2D.Double(i * L / faixas, 0, L / faixas + 1, A));
        }

        if (!comMarcacoes) return;

        g.setColor(LINHA);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Rectangle2D.Double(0, 0, L, A));
        g.draw(new Line2D.Double(L / 2, 0, L / 2, A));
        g.draw(new Ellipse2D.Double(L / 2 - 75, A / 2 - 75, 150, 150));
        g.fill(new Ellipse2D.Double(L / 2 - 5, A / 2 - 5, 10, 10));

        for (int lado = 0; lado < 2; lado++) {
            double x0 = lado == 0 ? 0 : L;
            double s = lado == 0 ? 1 : -1;
            double ga = Campo.GRANDE_AREA_PROF, gh = Campo.GRANDE_AREA_ALT;
            double pa = Campo.PEQUENA_AREA_PROF, ph = Campo.PEQUENA_AREA_ALT;
            g.draw(new Rectangle2D.Double(Math.min(x0, x0 + s * ga), A / 2 - gh / 2, ga, gh));
            g.draw(new Rectangle2D.Double(Math.min(x0, x0 + s * pa), A / 2 - ph / 2, pa, ph));
            // Meia-lua
            Arc2D arco = new Arc2D.Double(x0 + s * 105 - 60, A / 2 - 60, 120, 120,
                    lado == 0 ? -53 : 127, 106, Arc2D.OPEN);
            g.draw(arco);
            g.fill(new Ellipse2D.Double(x0 + s * 105 - 3, A / 2 - 3, 6, 6));
        }

        gol(g, 0, -1);
        gol(g, L, +1);
    }

    private static void gol(Graphics2D g, double xLinha, int s) {
        double prof = Campo.PROFUNDIDADE_GOL;
        double x = s < 0 ? xLinha - prof : xLinha;
        Rectangle2D rede = new Rectangle2D.Double(x, Campo.GOL_Y1, prof, Campo.BOCA_GOL);
        g.setColor(new Color(240, 240, 240, 60));
        g.fill(rede);
        g.setColor(new Color(255, 255, 255, 110));
        g.setStroke(new BasicStroke(1f));
        for (double yy = Campo.GOL_Y1; yy <= Campo.GOL_Y2; yy += 10) g.draw(new Line2D.Double(x, yy, x + prof, yy));
        for (double xx = x; xx <= x + prof; xx += 10) g.draw(new Line2D.Double(xx, Campo.GOL_Y1, xx, Campo.GOL_Y2));
        g.setColor(new Color(230, 40, 40));
        g.setStroke(new BasicStroke(3f));
        g.draw(rede);
        g.setColor(Color.WHITE);
        for (Obstaculo t : Campo.TRAVES) {
            if (Math.abs(t.centro().x() - xLinha) < 1) {
                circulo(g, t.centro(), t.raio(), true);
            }
        }
    }

    // ------------------------------ FICHAS ------------------------------

    public static void ficha(Graphics2D g, Ficha f, boolean destaque) {
        ficha(g, f.pos, f.raio, f.selecao, f.papel == Papel.GOLEIRO, destaque);
    }

    public static void ficha(Graphics2D g, Vetor p, double r, Selecao selecao, boolean goleiro, boolean destaque) {
        // Sombra
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(p.x() - r + r * 0.17, p.y() - r + r * 0.22, 2 * r, 2 * r));

        // Borda (corpo do botão)
        g.setColor(new Color(210, 210, 215));
        g.fill(new Ellipse2D.Double(p.x() - r, p.y() - r, 2 * r, 2 * r));
        g.setColor(new Color(120, 120, 130));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Ellipse2D.Double(p.x() - r, p.y() - r, 2 * r, 2 * r));

        double ri = r * 0.78;
        Shape miolo = new Ellipse2D.Double(p.x() - ri, p.y() - ri, 2 * ri, 2 * ri);

        // Bandeira, desenhada no quadrado [-1,1]² e recortada no círculo
        Graphics2D gb = (Graphics2D) g.create();
        gb.clip(miolo);
        gb.translate(p.x(), p.y());
        gb.scale(ri, ri);
        selecao.bandeira().desenhar(gb);
        gb.dispose();

        Shape clipAntigo = g.getClip();
        g.clip(miolo);
        // Brilho
        g.setPaint(new RadialGradientPaint(new Point2D.Double(p.x() - ri * 0.4, p.y() - ri * 0.5), (float) (ri * 1.4),
                new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 90), new Color(255, 255, 255, 0)}));
        g.fill(miolo);
        g.setClip(clipAntigo);

        if (goleiro) {
            g.setColor(new Color(255, 200, 0));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new Ellipse2D.Double(p.x() - r - 1, p.y() - r - 1, 2 * r + 2, 2 * r + 2));
        }
        if (destaque) {
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4, 4}, 0));
            g.draw(new Ellipse2D.Double(p.x() - r - 5, p.y() - r - 5, 2 * r + 10, 2 * r + 10));
        }
    }

    public static void bola(Graphics2D g, Bola b) {
        Vetor p = b.pos;
        double r = Bola.RAIO;
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new Ellipse2D.Double(p.x() - r + 2, p.y() - r + 3, 2 * r, 2 * r));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(p.x() - r, p.y() - r, 2 * r, 2 * r));
        g.setColor(new Color(30, 30, 30));
        g.fill(new Ellipse2D.Double(p.x() - 2.6, p.y() - 2.6, 5.2, 5.2));
        for (int i = 0; i < 5; i++) {
            Vetor q = p.soma(Vetor.porAngulo(i * 2 * Math.PI / 5 - Math.PI / 2).mul(r * 0.8));
            g.fill(new Ellipse2D.Double(q.x() - 1.8, q.y() - 1.8, 3.6, 3.6));
        }
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(p.x() - r, p.y() - r, 2 * r, 2 * r));
    }

    public static void cone(Graphics2D g, Obstaculo o) {
        Vetor c = o.centro();
        double r = o.raio();
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(c.x() - r + 3, c.y() - r + 4, 2 * r, 2 * r));
        g.setColor(new Color(255, 120, 20));
        g.fill(new Ellipse2D.Double(c.x() - r, c.y() - r, 2 * r, 2 * r));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(c.x() - r * 0.55, c.y() - r * 0.55, r * 1.1, r * 1.1));
        g.setColor(new Color(200, 80, 0));
        g.fill(new Ellipse2D.Double(c.x() - r * 0.25, c.y() - r * 0.25, r * 0.5, r * 0.5));
    }

    // ------------------------------ PETELECO ------------------------------

    /** Seta de mira do peteleco (igual à da imagem de referência). */
    public static void mira(Graphics2D g, Vetor origem, Vetor velocidade, double fracao, Vetor arraste) {
        // Pontilhado da puxada
        g.setColor(new Color(255, 255, 255, 170));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2, 6}, 0));
        g.draw(new Line2D.Double(origem.x(), origem.y(), arraste.x(), arraste.y()));

        // Círculo de alcance
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(new Ellipse2D.Double(origem.x() - 60, origem.y() - 60, 120, 120));

        if (velocidade.ehZero()) return;
        Color cor = new Color((int) (120 + 135 * fracao), (int) (220 - 160 * fracao), 40);
        seta(g, origem.soma(velocidade.comTamanho(22)), velocidade.comTamanho(30 + 70 * fracao), cor, 4f, 1f);
    }

    // ------------------------------ DEBUG ------------------------------

    public static void debugAgente(Graphics2D g, Agente a, boolean mostrarEstado) {
        Agente.Debug d = a.debug;
        Stroke fino = new BasicStroke(1.2f);
        Stroke tracejado = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{5, 4}, 0);

        if (d.alvo != null) {
            g.setColor(new Color(255, 255, 0, 200));
            g.setStroke(tracejado);
            g.draw(new Line2D.Double(a.pos.x(), a.pos.y(), d.alvo.x(), d.alvo.y()));
            xis(g, d.alvo, 5);
            if (d.raioDesaceleracao > 0) {
                g.setColor(new Color(255, 255, 0, 80));
                g.setStroke(tracejado);
                circulo(g, d.alvo, d.raioDesaceleracao, false);
            }
        }
        if (d.panicoOrigem != null && d.panicoRaio > 0) {
            g.setColor(new Color(255, 60, 60, 150));
            g.setStroke(tracejado);
            circulo(g, d.panicoOrigem, d.panicoRaio, false);
        }
        if (d.wanderCentro != null) {
            g.setColor(new Color(180, 255, 255, 190));
            g.setStroke(fino);
            circulo(g, d.wanderCentro, d.wanderRaio, false);
            g.draw(new Line2D.Double(a.pos.x(), a.pos.y(), d.wanderCentro.x(), d.wanderCentro.y()));
            g.fill(new Ellipse2D.Double(d.wanderAlvo.x() - 3, d.wanderAlvo.y() - 3, 6, 6));
        }
        if (d.caixaComprimento > 0) {
            // Caixa de detecção do Obstacle Avoidance (no espaço local do agente)
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(a.pos.x(), a.pos.y());
            g2.rotate(a.direcao.angulo());
            g2.setColor(d.obstaculoDetectado != null ? new Color(255, 80, 80, 160) : new Color(255, 255, 255, 70));
            g2.setStroke(fino);
            g2.draw(new Rectangle2D.Double(0, -a.raio, d.caixaComprimento, 2 * a.raio));
            g2.dispose();
            if (d.obstaculoDetectado != null) {
                g.setColor(new Color(255, 60, 60, 220));
                g.setStroke(new BasicStroke(2f));
                circulo(g, d.obstaculoDetectado.centro(), d.obstaculoDetectado.raio() + 4, false);
            }
        }
        if (d.caminhoFuturo != null) {
            g.setColor(new Color(255, 160, 255, 220));
            g.setStroke(fino);
            g.fill(new Ellipse2D.Double(d.caminhoFuturo.x() - 3, d.caminhoFuturo.y() - 3, 6, 6));
            g.draw(new Line2D.Double(d.caminhoFuturo.x(), d.caminhoFuturo.y(), d.caminhoProjecao.x(), d.caminhoProjecao.y()));
            g.setColor(new Color(255, 80, 255));
            xis(g, d.caminhoAlvo, 4);
        }

        // Velocidade (azul) e steering (laranja), como no template da professora
        Vetor origem = a.pos;
        seta(g, origem, a.vel.mul(0.3), new Color(40, 90, 255, 200), 2.5f, 1f);
        seta(g, origem, a.ultimoSteering.mul(0.1), new Color(255, 90, 0, 220), 2.5f, 1f);

        if (mostrarEstado && d.estado != null && !d.estado.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            String rotulo = d.estado;
            int largura = g.getFontMetrics().stringWidth(rotulo);
            float x = (float) (a.pos.x() - largura / 2.0);
            float y = (float) (a.pos.y() - a.raio - 8);
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(new RoundRectangle2D.Double(x - 3, y - 10, largura + 6, 13, 6, 6));
            g.setColor(Color.WHITE);
            g.drawString(rotulo, x, y);
        }
    }

    public static void caminho(Graphics2D g, Caminho c, Color cor) {
        Path2D p = new Path2D.Double();
        Vetor primeiro = c.pontos.get(0);
        p.moveTo(primeiro.x(), primeiro.y());
        for (int i = 1; i < c.pontos.size(); i++) p.lineTo(c.pontos.get(i).x(), c.pontos.get(i).y());
        if (c.fechado) p.closePath();
        g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 50));
        g.setStroke(new BasicStroke((float) (c.raio * 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
        g.setColor(cor);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8, 6}, 0));
        g.draw(p);
        for (Vetor v : c.pontos) g.fill(new Ellipse2D.Double(v.x() - 2.5, v.y() - 2.5, 5, 5));
    }

    // ------------------------------ PRIMITIVAS ------------------------------

    public static void seta(Graphics2D g, Vetor origem, Vetor v, Color cor, float espessura, float alfa) {
        if (v.tamanho() < 2) return;
        Vetor ponta = origem.soma(v);
        g.setColor(cor);
        g.setStroke(new BasicStroke(espessura, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(origem.x(), origem.y(), ponta.x(), ponta.y()));
        double ang = v.angulo();
        double cab = Math.min(10, v.tamanho() * 0.5);
        Vetor p1 = ponta.sub(Vetor.porAngulo(ang - 0.5).mul(cab));
        Vetor p2 = ponta.sub(Vetor.porAngulo(ang + 0.5).mul(cab));
        g.draw(new Line2D.Double(ponta.x(), ponta.y(), p1.x(), p1.y()));
        g.draw(new Line2D.Double(ponta.x(), ponta.y(), p2.x(), p2.y()));
    }

    public static void circulo(Graphics2D g, Vetor c, double r, boolean cheio) {
        Ellipse2D e = new Ellipse2D.Double(c.x() - r, c.y() - r, 2 * r, 2 * r);
        if (cheio) g.fill(e); else g.draw(e);
    }

    private static void xis(Graphics2D g, Vetor c, double r) {
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(c.x() - r, c.y() - r, c.x() + r, c.y() + r));
        g.draw(new Line2D.Double(c.x() - r, c.y() + r, c.x() + r, c.y() - r));
    }
}
