package futebol;

import futebol.motor.Vetor;
import futebol.selecoes.Selecao;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;

/** Tela inicial com os botões Jogar e Sair. */
public final class Menu {

    public final Botao jogar;
    public final Botao sair;
    private double tempo = 0;

    public Menu(int largura) {
        int w = 280, x = largura / 2 - w / 2;
        jogar = new Botao("JOGAR", x, 400, w, 64, new Color(0, 150, 60));
        sair = new Botao("SAIR", x, 485, w, 56, new Color(170, 40, 45));
    }

    public void atualizar(double dt) {
        tempo += dt;
    }

    public void desenhar(Graphics2D g, int largura, int altura, boolean somLigado) {
        // Fundo: o campo, escurecido
        Graphics2D gc = (Graphics2D) g.create();
        gc.translate(Tela.OX, Tela.OY);
        Desenho.campo(gc, true);
        gc.dispose();
        g.setColor(new Color(8, 14, 10, 175));
        g.fillRect(0, 0, largura, altura);

        // Fichas decorativas girando devagar em volta do título
        List<Selecao> enfeites = List.of(Selecao.BRASIL, Selecao.ADVERSARIOS.get(25), Selecao.ADVERSARIOS.get(37),
                Selecao.ADVERSARIOS.get(6), Selecao.ADVERSARIOS.get(41), Selecao.ADVERSARIOS.get(18));
        for (int i = 0; i < enfeites.size(); i++) {
            double a = tempo * 0.25 + i * 2 * Math.PI / enfeites.size();
            Vetor p = Vetor.de(largura / 2.0 + Math.cos(a) * 400, 250 + Math.sin(a) * 150);
            Desenho.ficha(g, p, 30, enfeites.get(i), false, false);
        }

        // Bola quicando sob o título
        double y = 300 - Math.abs(Math.sin(tempo * 3)) * 22;
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new Ellipse2D.Double(largura / 2.0 - 10, 312, 20, 7));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(largura / 2.0 - 11, y - 11, 22, 22));
        g.setColor(new Color(30, 30, 30));
        g.fill(new Ellipse2D.Double(largura / 2.0 - 4, y - 4, 8, 8));

        // Título
        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        String titulo = "FUTEBOL DE FICHA";
        int tw = g.getFontMetrics().stringWidth(titulo);
        g.setColor(new Color(0, 0, 0, 140));
        g.drawString(titulo, largura / 2 - tw / 2 + 4, 194);
        g.setPaint(new GradientPaint(0, 140, new Color(255, 223, 0), 0, 200, new Color(255, 170, 0)));
        g.drawString(titulo, largura / 2 - tw / 2, 190);

        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.setColor(new Color(220, 235, 225));
        String sub = "Steering Behaviors  •  Copa do Mundo 2026";
        g.drawString(sub, largura / 2 - g.getFontMetrics().stringWidth(sub) / 2, 232);

        jogar.desenhar(g);
        sair.desenhar(g);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(170, 185, 178));
        String dica = "Enter: jogar   •   Esc: sair   •   M: som " + (somLigado ? "(ligado)" : "(desligado)");
        g.drawString(dica, largura / 2 - g.getFontMetrics().stringWidth(dica) / 2, altura - 52);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String cred = "PUCPR — Inteligência Artificial e Ilusão de Inteligência em Jogos";
        g.drawString(cred, largura / 2 - g.getFontMetrics().stringWidth(cred) / 2, altura - 30);
    }
}
