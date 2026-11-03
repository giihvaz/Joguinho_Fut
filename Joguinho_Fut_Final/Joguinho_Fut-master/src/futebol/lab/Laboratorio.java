package futebol.lab;

import futebol.ia.Combinador;
import futebol.ia.Comportamentos;
import futebol.jogo.Campo;
import futebol.jogo.Ficha;
import futebol.jogo.Fisica;
import futebol.jogo.Papel;
import futebol.jogo.Time;
import futebol.motor.Caminho;
import futebol.motor.Obstaculo;
import futebol.motor.Vetor;

import java.util.List;

/**
 * Mapa de testes: uma única ficha, cones (obstáculos) e um caminho fechado.
 * Cada tecla de 1 a 8 ativa um comportamento isolado ou uma combinação,
 * para facilitar a demonstração na arguição.
 */
public class Laboratorio {

    public static final String[] MODOS = {
            "",
            "1  Seek: persegue o cursor do mouse",
            "2  Flee: foge do cursor quando ele entra no raio de pânico",
            "3  Arrive: vai até o último clique e para suavemente",
            "4  Wander: passeia aleatoriamente de forma suave",
            "5  Obstacle Avoidance: desvia dos cones (Seek no mouse só dá a direção)",
            "6  Path Following: segue o caminho (sem desvio, ela bate nos cones!)",
            "7  COMBINAÇÃO: Path Following + Obstacle Avoidance",
            "8  COMBINAÇÃO: Wander + Obstacle Avoidance + Flee (ficha arisca)",
    };

    public static final String[] PESOS = {
            "",
            "Seek x1.0",
            "Contenção x3.0 > Flee x1.0 (raio de pânico 180 px)",
            "Arrive x1.0 (raio de desaceleração 140 px)",
            "Contenção x3.0 > Wander x1.0",
            "Obstacle Avoidance x1.6 > Seek x1.0",
            "Path Following x1.0",
            "Obstacle Avoidance x1.6 > Path Following x1.0",
            "Contenção x3.0 > Flee x1.5 > Obstacle Avoidance x1.6 > Wander x1.0",
    };

    public static final double RAIO_PANICO = 180;
    public static final double RAIO_ARRIVE = 140;

    public final Ficha ficha = new Ficha(Time.BRASIL, futebol.selecoes.Selecao.BRASIL, Papel.ATACANTE, 0, Vetor.de(180, 120), Vetor.de(180, 120));

    public final List<Obstaculo> cones = List.of(
            cone(500, 92), cone(862, 205), cone(500, 488), cone(140, 375),
            cone(380, 250), cone(530, 320), cone(660, 230), cone(300, 370), cone(700, 390), cone(460, 185)
    );

    public final Caminho caminho = new Caminho(List.of(
            Vetor.de(180, 120), Vetor.de(500, 90), Vetor.de(820, 120), Vetor.de(900, 290),
            Vetor.de(820, 460), Vetor.de(500, 490), Vetor.de(180, 460), Vetor.de(100, 290)
    ), true, 18);

    public int modo = 1;
    public Vetor mouse = Vetor.de(700, 300);
    public Vetor clique = null;

    private static Obstaculo cone(double x, double y) {
        return new Obstaculo(Vetor.de(x, y), 16);
    }

    public void trocarModo(int novo) {
        if (novo < 1 || novo >= MODOS.length) return;
        modo = novo;
        ficha.voltarParaFormacao();
    }

    public boolean usaCaminho() { return modo == 6 || modo == 7; }

    public void atualizar(double dt) {
        ficha.debug.limpar();
        Combinador c = new Combinador(ficha.maxForca);
        double m = ficha.raio + 10;

        switch (modo) {
            case 1 -> c.add("Seek", Comportamentos.seek(ficha, mouse), 1.0);
            case 2 -> {
                c.add("Conter", Comportamentos.conter(ficha, m, m, Campo.LARGURA - m, Campo.ALTURA - m), 3.0);
                c.add("Flee", Comportamentos.flee(ficha, mouse, RAIO_PANICO), 1.0);
            }
            case 3 -> {
                Vetor alvo = clique != null ? clique : Campo.CENTRO;
                c.add("Arrive", Comportamentos.arrive(ficha, alvo, RAIO_ARRIVE), 1.0);
            }
            case 4 -> {
                c.add("Conter", Comportamentos.conter(ficha, m, m, Campo.LARGURA - m, Campo.ALTURA - m), 3.0);
                c.add("Wander", Comportamentos.wander(ficha, dt, 60, 30, 2.6, 160), 1.0);
            }
            case 5 -> {
                c.add("OA", Comportamentos.evitarObstaculos(ficha, cones, 60), 1.6);
                c.add("Seek", Comportamentos.seek(ficha, mouse), 1.0);
            }
            case 6 -> c.add("Path", Comportamentos.seguirCaminho(ficha, caminho, 40, 35), 1.0);
            case 7 -> {
                c.add("OA", Comportamentos.evitarObstaculos(ficha, cones, 60), 1.6);
                c.add("Path", Comportamentos.seguirCaminho(ficha, caminho, 40, 35), 1.0);
            }
            case 8 -> {
                c.add("Conter", Comportamentos.conter(ficha, m, m, Campo.LARGURA - m, Campo.ALTURA - m), 3.0);
                c.add("Flee", Comportamentos.flee(ficha, mouse, RAIO_PANICO), 1.5);
                c.add("OA", Comportamentos.evitarObstaculos(ficha, cones, 60), 1.6);
                c.add("Wander", Comportamentos.wander(ficha, dt, 60, 30, 2.6, 160), 1.0);
            }
            default -> { }
        }
        ficha.debug.estado = c.descricao();
        ficha.atualizar(c.resultado(), dt);

        for (Obstaculo o : cones) Fisica.colidirFixo(ficha, o, 0.3);
        Fisica.conterNoRetangulo(ficha, 0, 0, Campo.LARGURA, Campo.ALTURA, 0.3);
    }
}
