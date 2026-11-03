package futebol.jogo;

import futebol.ia.Pesos;
import futebol.motor.Agente;
import futebol.motor.Vetor;
import futebol.selecoes.Selecao;

/** Uma ficha (botão) do futebol de mesa: um agente autônomo com time e papel. */
public class Ficha extends Agente implements Corpo {

    public final Time time;
    /** Seleção (bandeira desenhada na ficha). */
    public final Selecao selecao;
    public final Papel papel;
    /** Índice dentro do papel (ex.: zagueiro 0 = de cima, 1 = de baixo). */
    public final int indice;
    /** Posição na formação inicial (saída de bola). */
    public final Vetor posInicial;
    /** Centro da zona onde o atacante espera (usado pelo Wander com coleira). */
    public final Vetor zonaBase;
    /** Tempo até a IA poder dar outro peteleco (chute). */
    public double recargaChute = 0;
    /** Há quanto tempo o ativo está tentando se posicionar atrás da bola. */
    public double tempoPosicionando = 0;

    public Ficha(Time time, Selecao selecao, Papel papel, int indice, Vetor posInicial, Vetor zonaBase) {
        super(posInicial,
                Pesos.FICHA_MASSA,
                papel == Papel.GOLEIRO ? Pesos.GOLEIRO_MAX_FORCA : Pesos.FICHA_MAX_FORCA,
                papel == Papel.GOLEIRO ? Pesos.GOLEIRO_MAX_VEL : Pesos.FICHA_MAX_VEL,
                Pesos.FICHA_RAIO);
        this.time = time;
        this.selecao = selecao;
        this.papel = papel;
        this.indice = indice;
        this.posInicial = posInicial;
        this.zonaBase = zonaBase;
        this.direcao = Vetor.de(time.sentido, 0);
    }

    public void voltarParaFormacao() {
        pos = posInicial;
        vel = Vetor.ZERO;
        tempoLivre = 0;
        direcao = Vetor.de(time.sentido, 0);
        wanderAngulo = 0;
        recargaChute = 0;
        tempoPosicionando = 0;
    }

    @Override public Vetor getPos() { return pos; }
    @Override public void setPos(Vetor p) { pos = p; }
    @Override public Vetor getVel() { return vel; }
    @Override public void setVel(Vetor v) { vel = v; }
    @Override public double getRaio() { return raio; }
    @Override public double getMassa() { return massa; }
}
