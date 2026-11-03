package futebol.jogo;

import futebol.motor.Obstaculo;
import futebol.motor.Vetor;

/**
 * Colisões entre círculos. Isto NÃO é steering: é a física do mundo, que
 * resolve o que acontece quando dois corpos se tocam (o Obstacle Avoidance
 * tenta justamente evitar que isso aconteça entre as fichas).
 */
public final class Fisica {
    private Fisica() { }

    /**
     * Colisão entre dois corpos móveis: separa as sobreposições
     * (proporcional à massa) e troca impulso ao longo da normal.
     *
     * @return o impulso aplicado (0 se não houve choque)
     */
    public static double colidir(Corpo a, Corpo b, double restituicao) {
        Vetor delta = b.getPos().sub(a.getPos());
        double dist = delta.tamanho();
        double minimo = a.getRaio() + b.getRaio();
        if (dist >= minimo) return 0;

        Vetor normal = dist < 1e-6 ? Vetor.de(1, 0) : delta.div(dist);
        double invA = 1 / a.getMassa(), invB = 1 / b.getMassa();
        double sobreposicao = minimo - dist;

        // Corrige a posição (cada um anda proporcional ao inverso da massa).
        a.setPos(a.getPos().sub(normal.mul(sobreposicao * invA / (invA + invB))));
        b.setPos(b.getPos().soma(normal.mul(sobreposicao * invB / (invA + invB))));

        // Impulso só se estão se aproximando.
        double vRel = b.getVel().sub(a.getVel()).dot(normal);
        if (vRel >= 0) return 0;
        double j = -(1 + restituicao) * vRel / (invA + invB);
        a.setVel(a.getVel().sub(normal.mul(j * invA)));
        b.setVel(b.getVel().soma(normal.mul(j * invB)));
        return j;
    }

    /** Colisão de um corpo móvel com um obstáculo fixo (trave, cone). */
    public static void colidirFixo(Corpo c, Obstaculo o, double restituicao) {
        Vetor delta = c.getPos().sub(o.centro());
        double dist = delta.tamanho();
        double minimo = c.getRaio() + o.raio();
        if (dist >= minimo) return;
        Vetor normal = dist < 1e-6 ? Vetor.de(1, 0) : delta.div(dist);
        c.setPos(o.centro().soma(normal.mul(minimo)));
        double vn = c.getVel().dot(normal);
        if (vn < 0) c.setVel(c.getVel().sub(normal.mul((1 + restituicao) * vn)));
    }

    /** Mantém um corpo dentro do retângulo, rebatendo nas paredes. */
    public static void conterNoRetangulo(Corpo c, double minX, double minY, double maxX, double maxY, double restituicao) {
        Vetor p = c.getPos(), v = c.getVel();
        double r = c.getRaio();
        double x = p.x(), y = p.y(), vx = v.x(), vy = v.y();
        if (x < minX + r) { x = minX + r; if (vx < 0) vx = -vx * restituicao; }
        if (x > maxX - r) { x = maxX - r; if (vx > 0) vx = -vx * restituicao; }
        if (y < minY + r) { y = minY + r; if (vy < 0) vy = -vy * restituicao; }
        if (y > maxY - r) { y = maxY - r; if (vy > 0) vy = -vy * restituicao; }
        c.setPos(Vetor.de(x, y));
        c.setVel(Vetor.de(vx, vy));
    }
}
