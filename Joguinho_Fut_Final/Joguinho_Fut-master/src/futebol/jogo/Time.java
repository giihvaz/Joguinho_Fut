package futebol.jogo;

/** Lados da partida. O Brasil defende o gol da esquerda e ataca para a direita. */
public enum Time {
    BRASIL(+1),
    /** A seleção sorteada na roleta (ver {@link futebol.selecoes.Selecao}). */
    ADVERSARIO(-1);

    /** +1 se ataca para a direita, -1 se ataca para a esquerda. */
    public final int sentido;

    Time(int sentido) {
        this.sentido = sentido;
    }

    /** x da linha do gol que este time DEFENDE. */
    public double linhaGolProprio() {
        return sentido > 0 ? 0 : Campo.LARGURA;
    }

    /** x da linha do gol que este time ATACA. */
    public double linhaGolAdversario() {
        return sentido > 0 ? Campo.LARGURA : 0;
    }

    /** Espelha um x "do ponto de vista do Brasil" para este time. */
    public double espelharX(double x) {
        return sentido > 0 ? x : Campo.LARGURA - x;
    }
}
