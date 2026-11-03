package futebol.motor;

import java.util.List;

/**
 * Caminho formado por uma sequência de pontos (polilinha), aberto ou fechado,
 * com um "raio" que define a largura do corredor usado pelo Path Following.
 */
public final class Caminho {

    public final List<Vetor> pontos;
    public final boolean fechado;
    public final double raio;

    public Caminho(List<Vetor> pontos, boolean fechado, double raio) {
        if (pontos.size() < 2) throw new IllegalArgumentException("caminho precisa de 2+ pontos");
        this.pontos = List.copyOf(pontos);
        this.fechado = fechado;
        this.raio = raio;
    }

    public int quantidadeSegmentos() {
        return fechado ? pontos.size() : pontos.size() - 1;
    }

    public Vetor inicio(int seg) { return pontos.get(seg); }
    public Vetor fim(int seg) { return pontos.get((seg + 1) % pontos.size()); }

    /** Resultado da projeção de um ponto no caminho. */
    public record Projecao(Vetor ponto, int segmento, double distancia) { }

    /** Encontra o ponto do caminho mais próximo de {@code p} (projeção ortogonal em cada segmento). */
    public Projecao projetar(Vetor p) {
        Projecao melhor = null;
        for (int i = 0; i < quantidadeSegmentos(); i++) {
            Vetor a = inicio(i), b = fim(i);
            Vetor ab = b.sub(a);
            double t = p.sub(a).dot(ab) / Math.max(ab.tamanho2(), 1e-9);
            t = Math.max(0, Math.min(1, t));
            Vetor q = a.soma(ab.mul(t));
            double d = q.distancia(p);
            if (melhor == null || d < melhor.distancia) melhor = new Projecao(q, i, d);
        }
        return melhor;
    }

    /**
     * Anda {@code distancia} pixels ao longo do caminho, a partir do ponto
     * {@code ponto} que está no segmento {@code seg}, passando para os próximos
     * segmentos quando necessário.
     */
    public Vetor avancar(int seg, Vetor ponto, double distancia) {
        Vetor atual = ponto;
        int s = seg;
        double resto = distancia;
        for (int guarda = 0; guarda < pontos.size() * 2; guarda++) {
            Vetor fimSeg = fim(s);
            double ate = atual.distancia(fimSeg);
            if (resto <= ate) {
                return atual.soma(fimSeg.sub(atual).comTamanho(resto));
            }
            resto -= ate;
            atual = fimSeg;
            s++;
            if (s >= quantidadeSegmentos()) {
                if (!fechado) return pontos.get(pontos.size() - 1);
                s = 0;
            }
        }
        return atual;
    }
}
