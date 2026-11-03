package futebol.jogo;

import futebol.motor.Obstaculo;
import futebol.motor.Vetor;

import java.util.List;

/** Dimensões do campo (coordenadas do mundo: (0,0) no canto superior esquerdo do gramado). */
public final class Campo {
    private Campo() { }

    public static final double LARGURA = 1000;
    public static final double ALTURA = 580;
    public static final double CENTRO_Y = ALTURA / 2;

    public static final double BOCA_GOL = 170;
    public static final double GOL_Y1 = CENTRO_Y - BOCA_GOL / 2;
    public static final double GOL_Y2 = CENTRO_Y + BOCA_GOL / 2;
    public static final double PROFUNDIDADE_GOL = 34;
    public static final double RAIO_TRAVE = 6;

    public static final double GRANDE_AREA_PROF = 150;
    public static final double GRANDE_AREA_ALT = 340;
    public static final double PEQUENA_AREA_PROF = 55;
    public static final double PEQUENA_AREA_ALT = 200;

    public static final Vetor CENTRO = Vetor.de(LARGURA / 2, CENTRO_Y);

    /** As quatro traves são obstáculos circulares fixos. */
    public static final List<Obstaculo> TRAVES = List.of(
            new Obstaculo(Vetor.de(0, GOL_Y1), RAIO_TRAVE),
            new Obstaculo(Vetor.de(0, GOL_Y2), RAIO_TRAVE),
            new Obstaculo(Vetor.de(LARGURA, GOL_Y1), RAIO_TRAVE),
            new Obstaculo(Vetor.de(LARGURA, GOL_Y2), RAIO_TRAVE)
    );

    public static boolean naBocaDoGol(double y) {
        return y > GOL_Y1 && y < GOL_Y2;
    }

    public static Vetor centroDoGol(double xLinha) {
        return Vetor.de(xLinha, CENTRO_Y);
    }
}
