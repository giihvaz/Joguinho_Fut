package futebol.selecoes;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Bandeira simplificada, descrita como uma lista de formas num quadrado
 * [-1, 1] x [-1, 1] (y para baixo). A ficha recorta esse quadrado num círculo,
 * por isso os desenhos são versões simplificadas, centradas no que aparece.
 */
public final class Bandeira {

    private interface Forma {
        void desenhar(Graphics2D g);
    }

    private final List<Forma> formas = new ArrayList<>();

    /** Desenha a bandeira já transformada para o quadrado unitário. */
    public void desenhar(Graphics2D g) {
        for (Forma f : formas) f.desenhar(g);
    }

    /** Cor média aproximada (usada em detalhes da interface). */
    public Color corPrincipal = Color.GRAY;

    // ------------------------------------------------------------------
    // Construtores de formas (API fluente)
    // ------------------------------------------------------------------

    public static Bandeira nova() { return new Bandeira(); }

    public Bandeira fundo(Color c) {
        if (formas.isEmpty()) corPrincipal = c;
        return ret(-1, -1, 2, 2, c);
    }

    /** Faixas horizontais iguais (de cima para baixo). */
    public Bandeira horizontais(Color... cores) {
        double[] pesos = new double[cores.length];
        java.util.Arrays.fill(pesos, 1);
        return horizontais(pesos, cores);
    }

    public Bandeira horizontais(double[] pesos, Color... cores) {
        double total = 0;
        for (double p : pesos) total += p;
        double y = -1;
        for (int i = 0; i < cores.length; i++) {
            double h = 2 * pesos[i] / total;
            ret(-1, y, 2, h + (i < cores.length - 1 ? 0.01 : 0), cores[i]);
            y += h;
        }
        corPrincipal = cores[0];
        return this;
    }

    /** Faixas verticais iguais (da esquerda para a direita). */
    public Bandeira verticais(Color... cores) {
        double[] pesos = new double[cores.length];
        java.util.Arrays.fill(pesos, 1);
        return verticais(pesos, cores);
    }

    public Bandeira verticais(double[] pesos, Color... cores) {
        double total = 0;
        for (double p : pesos) total += p;
        double x = -1;
        for (int i = 0; i < cores.length; i++) {
            double w = 2 * pesos[i] / total;
            ret(x, -1, w + (i < cores.length - 1 ? 0.01 : 0), 2, cores[i]);
            x += w;
        }
        corPrincipal = cores[0];
        return this;
    }

    public Bandeira ret(double x, double y, double w, double h, Color c) {
        formas.add(g -> {
            g.setColor(c);
            g.fill(new Rectangle2D.Double(x, y, w, h));
        });
        return this;
    }

    public Bandeira circulo(double cx, double cy, double r, Color c) {
        formas.add(g -> {
            g.setColor(c);
            g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        });
        return this;
    }

    public Bandeira anel(double cx, double cy, double r, double espessura, Color c) {
        formas.add(g -> {
            g.setColor(c);
            g.setStroke(new BasicStroke((float) espessura));
            g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        });
        return this;
    }

    /** Polígono a partir de pares x,y. */
    public Bandeira poligono(Color c, double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.closePath();
        formas.add(g -> {
            g.setColor(c);
            g.fill(p);
        });
        return this;
    }

    public Bandeira estrela(double cx, double cy, double r, Color c) {
        return estrela(cx, cy, r, 5, c, false);
    }

    public Bandeira estrela(double cx, double cy, double r, int pontas, Color c, boolean contorno) {
        Path2D p = new Path2D.Double();
        double interno = r * (pontas == 5 ? 0.4 : 0.5);
        for (int i = 0; i < pontas * 2; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / pontas;
            double rr = i % 2 == 0 ? r : interno;
            double x = cx + rr * Math.cos(ang), y = cy + rr * Math.sin(ang);
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        formas.add(g -> {
            g.setColor(c);
            if (contorno) {
                // Pentagrama "entrelaçado" (como no Marrocos): ligar pontas alternadas.
                Path2D pent = new Path2D.Double();
                for (int i = 0; i <= 5; i++) {
                    double ang = -Math.PI / 2 + (i * 2 % 5) * 2 * Math.PI / 5;
                    double x = cx + r * Math.cos(ang), y = cy + r * Math.sin(ang);
                    if (i == 0) pent.moveTo(x, y); else pent.lineTo(x, y);
                }
                g.setStroke(new BasicStroke((float) (r * 0.16), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(pent);
            } else {
                g.fill(p);
            }
        });
        return this;
    }

    /** Lua crescente: um círculo da cor, "mordido" por outro da cor de fundo. */
    public Bandeira crescente(double cx, double cy, double r, double deslocamento, Color c, Color fundo) {
        circulo(cx, cy, r, c);
        return circulo(cx + deslocamento, cy, r * 0.8, fundo);
    }

    /** Cruz centralizada (Inglaterra, Suíça). */
    public Bandeira cruz(double tamanho, double espessura, Color c) {
        ret(-tamanho, -espessura / 2, 2 * tamanho, espessura, c);
        return ret(-espessura / 2, -tamanho, espessura, 2 * tamanho, c);
    }

    /** Cruz nórdica (deslocada para a esquerda). */
    public Bandeira cruzNordica(Color c, double espessura) {
        double x = -0.3;
        ret(-1, -espessura / 2, 2, espessura, c);
        return ret(x - espessura / 2, -1, espessura, 2, c);
    }

    /** Aspa / cruz em X (Escócia). */
    public Bandeira aspa(Color c, double espessura) {
        formas.add(g -> {
            g.setColor(c);
            g.setStroke(new BasicStroke((float) espessura, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g.draw(new Line2D.Double(-1.2, -1.2, 1.2, 1.2));
            g.draw(new Line2D.Double(-1.2, 1.2, 1.2, -1.2));
        });
        return this;
    }

    /** Sol com raios (Argentina, Uruguai). */
    public Bandeira sol(double cx, double cy, double r, Color c) {
        formas.add(g -> {
            g.setColor(c);
            g.setStroke(new BasicStroke((float) (r * 0.18)));
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                g.draw(new Line2D.Double(cx + Math.cos(a) * r * 0.6, cy + Math.sin(a) * r * 0.6,
                        cx + Math.cos(a) * r * 1.35, cy + Math.sin(a) * r * 1.35));
            }
        });
        return circulo(cx, cy, r * 0.65, c);
    }

    /** "Union Jack" bem simplificada, num retângulo (cantão de Austrália/Nova Zelândia). */
    public Bandeira uniao(double x, double y, double w, double h) {
        Color azul = new Color(1, 33, 105), vermelho = new Color(200, 16, 46);
        ret(x, y, w, h, azul);
        formas.add(g -> {
            Shape clipAntigo = g.getClip();
            g.clip(new Rectangle2D.Double(x, y, w, h));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke((float) (h * 0.2)));
            g.draw(new Line2D.Double(x, y, x + w, y + h));
            g.draw(new Line2D.Double(x, y + h, x + w, y));
            g.setColor(vermelho);
            g.setStroke(new BasicStroke((float) (h * 0.08)));
            g.draw(new Line2D.Double(x, y, x + w, y + h));
            g.draw(new Line2D.Double(x, y + h, x + w, y));
            g.setClip(clipAntigo);
        });
        ret(x, y + h * 0.36, w, h * 0.28, Color.WHITE);
        ret(x + w * 0.38, y, w * 0.24, h, Color.WHITE);
        ret(x, y + h * 0.42, w, h * 0.16, vermelho);
        return ret(x + w * 0.43, y, w * 0.14, h, vermelho);
    }
}
