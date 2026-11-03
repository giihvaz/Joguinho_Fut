package futebol.ia;

/**
 * Todos os pesos e parâmetros ajustáveis dos comportamentos, reunidos em um
 * único lugar. As justificativas de cada valor estão no README.
 *
 * Lembrete: no {@link Combinador} a ORDEM também importa (prioridade).
 * A ordem usada em todos os papéis é:
 *   1. Contenção (não sair da região)  2. Flee  3. Obstacle Avoidance  4. Objetivo do papel
 */
public final class Pesos {
    private Pesos() { }

    // ---------------- Física das fichas ----------------
    public static final double FICHA_MASSA = 1.0;
    public static final double FICHA_MAX_FORCA = 650;
    public static final double FICHA_MAX_VEL = 210;
    public static final double GOLEIRO_MAX_FORCA = 750;
    public static final double GOLEIRO_MAX_VEL = 185;
    public static final double FICHA_RAIO = 18;

    // ---------------- Pesos comuns ----------------
    public static final double CONTER = 3.0;
    public static final double FLEE_FICHA_LANCADA = 1.5;
    public static final double RAIO_PANICO = 110;
    /** Velocidade mínima para uma ficha lançada ser considerada ameaça. */
    public static final double VEL_AMEACA = 260;

    // ---------------- Atacante ativo (vai na bola) ----------------
    public static final double ATIVO_ARRIVE_POSICIONAR = 1.0;
    public static final double ATIVO_RAIO_POSICIONAR = 60;
    public static final double ATIVO_OA_CONTORNAR = 1.6;
    public static final double ATIVO_SEEK_CHUTE = 1.0;
    public static final double ATIVO_OA_CHUTE = 0.7;
    public static final double ATIVO_ALINHAMENTO = 0.88;   // cos(~28°)
    public static final double OA_CAIXA_ATIVO = 40;
    /** Distância extra atrás da bola onde o ativo se posiciona antes do chute. */
    public static final double ATIVO_RECUO = 22;
    public static final double ATIVO_DIST_PETELECO = 75;
    public static final double ATIVO_FORCA_PETELECO = 480;
    public static final double ATIVO_ERRO_MIRA = 0.07;      // rad
    public static final double ATIVO_RECARGA_PETELECO = 1.2; // s
    public static final double ATIVO_RAIO_DISPUTA = 48;
    public static final double ATIVO_TEMPO_MAX_POSICIONAR = 1.6;

    // ---------------- Zagueiro (cobertura) ----------------
    public static final double ZAG_ARRIVE_COBERTURA = 1.0;
    public static final double ZAG_RAIO_DESACELERACAO = 90;
    public static final double ZAG_OA = 1.5;
    public static final double OA_CAIXA_PADRAO = 55;

    // ---------------- Atacante de apoio / espera ----------------
    public static final double APOIO_ARRIVE = 1.0;
    public static final double APOIO_OA = 1.5;
    public static final double ESPERA_WANDER = 1.0;
    public static final double ESPERA_COLEIRA = 0.7;
    public static final double ESPERA_RAIO_COLEIRA = 85;
    public static final double WANDER_DISTANCIA = 45;
    public static final double WANDER_RAIO = 22;
    public static final double WANDER_JITTER = 2.6;
    public static final double WANDER_VEL = 110;

    // ---------------- Goleiro ----------------
    public static final double GOL_PATRULHA_PATH = 1.0;
    public static final double GOL_PATRULHA_VEL = 95;
    public static final double GOL_AMEACA_ARRIVE = 1.0;
    public static final double GOL_AMEACA_PATH = 0.25;
    public static final double GOL_SAIDA_SEEK = 1.0;
    public static final double GOL_DIST_AMEACA = 330;
    public static final double GOL_DIST_SAIDA = 80;
}
