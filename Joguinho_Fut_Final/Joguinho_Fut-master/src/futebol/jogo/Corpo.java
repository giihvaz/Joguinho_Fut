package futebol.jogo;

import futebol.motor.Vetor;

/** Qualquer coisa redonda que colide: fichas e bola. */
public interface Corpo {
    Vetor getPos();
    void setPos(Vetor p);
    Vetor getVel();
    void setVel(Vetor v);
    double getRaio();
    double getMassa();
}
