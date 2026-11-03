package futebol;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Botão simples desenhado na tela (com efeito de "hover"). */
public final class Botao {

    public final String texto;
    public final Rectangle area;
    private final Color cor;
    public boolean sobre = false;

    public Botao(String texto, int x, int y, int largura, int altura, Color cor) {
        this.texto = texto;
        this.area = new Rectangle(x, y, largura, altura);
        this.cor = cor;
    }

    public boolean contem(int x, int y) {
        return area.contains(x, y);
    }

    public void desenhar(Graphics2D g) {
        int dy = sobre ? -2 : 0;
        RoundRectangle2D forma = new RoundRectangle2D.Double(area.x, area.y + dy, area.width, area.height, 18, 18);

        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(area.x + 3, area.y + 5, area.width, area.height, 18, 18));

        Color base = sobre ? cor.brighter() : cor;
        g.setPaint(new GradientPaint(0, area.y + dy, base, 0, area.y + dy + area.height, base.darker()));
        g.fill(forma);
        g.setColor(new Color(255, 255, 255, sobre ? 220 : 120));
        g.setStroke(new BasicStroke(2f));
        g.draw(forma);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(texto, area.x + (area.width - fm.stringWidth(texto)) / 2,
                area.y + dy + (area.height + fm.getAscent() - fm.getDescent()) / 2);
    }
}
