package futebol.jogo;

import futebol.motor.Vetor;

/** A bola não tem IA: só física (inércia + atrito de rolamento). */
public class Bola implements Corpo {

    public static final double RAIO = 8;
    public static final double MASSA = 0.25;
    public static final double ATRITO = 0.55;      // 1/s
    public static final double VEL_MAXIMA = 1100;

    public Vetor pos = Vetor.ZERO;
    public Vetor vel = Vetor.ZERO;

    public void atualizar(double dt) {
        vel = vel.mul(Math.exp(-ATRITO * dt)).truncado(VEL_MAXIMA);
        if (vel.tamanho() < 4) vel = Vetor.ZERO;
        pos = pos.soma(vel.mul(dt));
    }

    /** Posição estimada daqui a {@code t} segundos (ignora atrito; usado pela IA). */
    public Vetor prever(double t) {
        return pos.soma(vel.mul(t));
    }

    @Override public Vetor getPos() { return pos; }
    @Override public void setPos(Vetor p) { pos = p; }
    @Override public Vetor getVel() { return vel; }
    @Override public void setVel(Vetor v) { vel = v; }
    @Override public double getRaio() { return RAIO; }
    @Override public double getMassa() { return MASSA; }
}
