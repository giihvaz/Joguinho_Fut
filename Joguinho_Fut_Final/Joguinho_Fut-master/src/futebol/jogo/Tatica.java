package futebol.jogo;

import futebol.ia.Combinador;
import futebol.ia.Comportamentos;
import futebol.motor.Caminho;
import futebol.motor.Obstaculo;
import futebol.motor.Vetor;

import java.util.ArrayList;
import java.util.List;

import static futebol.ia.Pesos.*;

/**
 * Decide, para cada ficha controlada pela IA, QUAIS comportamentos combinar e
 * com QUE pesos, de acordo com o papel e a situação do jogo.
 *
 * <p>Combinações usadas no jogo (detalhadas no README):</p>
 * <ol>
 *   <li><b>Atacante ativo</b>: Arrive (posicionar atrás da bola) + Obstacle Avoidance
 *       (contornar a bola e as fichas) e, quando alinhado, Seek (chute) + Obstacle Avoidance.</li>
 *   <li><b>Zagueiro</b>: Flee (fichas lançadas) + Obstacle Avoidance + Arrive (cobertura).</li>
 *   <li><b>Goleiro</b>: Path Following (patrulha) e, sob ameaça, Arrive (interceptar) + Path Following.</li>
 *   <li><b>Atacante em espera</b>: Wander + Arrive como "coleira" + Obstacle Avoidance + Flee.</li>
 * </ol>
 */
public final class Tatica {

    private static final java.util.Random RND = new java.util.Random();

    private final Partida partida;
    private final Caminho caminhoGoleiroBrasil;
    private final Caminho caminhoGoleiroAdversario;

    public Tatica(Partida partida) {
        this.partida = partida;
        this.caminhoGoleiroBrasil = criarCaminhoGoleiro(Time.BRASIL);
        this.caminhoGoleiroAdversario = criarCaminhoGoleiro(Time.ADVERSARIO);
    }

    public Caminho caminhoGoleiro(Time t) {
        return t == Time.BRASIL ? caminhoGoleiroBrasil : caminhoGoleiroAdversario;
    }

    /** Laço estreito (elipse achatada) na frente do gol: o goleiro patrulha a boca do gol. */
    private static Caminho criarCaminhoGoleiro(Time t) {
        double cx = t.linhaGolProprio() + t.sentido * 34;
        double rx = 12, ry = 72;
        List<Vetor> pts = new ArrayList<>();
        int n = 16;
        for (int i = 0; i < n; i++) {
            double ang = 2 * Math.PI * i / n;
            pts.add(Vetor.de(cx + rx * Math.cos(ang), Campo.CENTRO_Y + ry * Math.sin(ang)));
        }
        return new Caminho(pts, true, 6);
    }

    // ------------------------------------------------------------------

    public Vetor calcular(Ficha f, double dt) {
        f.debug.limpar();
        Combinador c = new Combinador(f.maxForca);

        Vetor resultado = switch (f.papel) {
            case GOLEIRO -> goleiro(f, c);
            case ZAGUEIRO -> f == partida.ativo(f.time) ? atacanteAtivo(f, c, dt) : zagueiro(f, c);
            case ATACANTE -> f == partida.ativo(f.time) ? atacanteAtivo(f, c, dt) : atacanteSemBola(f, c, dt);
        };
        if (!c.descricao().isEmpty()) f.debug.estado += "  [" + c.descricao() + "]";
        return resultado;
    }

    // ------------------------------ GOLEIRO ------------------------------

    private Vetor goleiro(Ficha f, Combinador c) {
        Time t = f.time;
        Bola bola = partida.bola;
        double linha = t.linhaGolProprio();
        double s = t.sentido;

        // Contenção: o goleiro não sai da área.
        double x1 = linha, x2 = linha + s * (Campo.GRANDE_AREA_PROF - 20);
        c.add("Conter", Comportamentos.conter(f, Math.min(x1, x2), Campo.CENTRO_Y - 150,
                Math.max(x1, x2), Campo.CENTRO_Y + 150), CONTER);

        double distBolaGol = Math.abs(bola.pos.x() - linha);
        boolean bolaNaFrente = s * (bola.pos.x() - linha) > 0;
        Caminho caminho = caminhoGoleiro(t);

        // Só sai se estiver ENTRE a bola e o gol (senão empurraria a bola contra o próprio fundo).
        boolean entreBolaEGol = s * (bola.pos.x() - f.pos.x()) > 4;
        if (bolaNaFrente && entreBolaEGol && f.pos.distancia(bola.pos) < GOL_DIST_SAIDA) {
            // Bola muito perto: sai para afastar (Seek).
            f.debug.estado = "GOLEIRO: saída";
            c.add("Seek", Comportamentos.seek(f, bola.pos.soma(Vetor.de(s * 10, 0))), GOL_SAIDA_SEEK);
        } else if (distBolaGol < GOL_DIST_AMEACA) {
            // Ameaça: posiciona-se na linha da bola (Arrive), ainda puxado pelo caminho.
            f.debug.estado = "GOLEIRO: ameaça";
            Vetor previsao = bola.prever(0.3);
            double y = Math.max(Campo.CENTRO_Y - 75, Math.min(Campo.CENTRO_Y + 75, previsao.y()));
            Vetor alvo = Vetor.de(linha + s * 32, y);
            c.add("Arrive", Comportamentos.arrive(f, alvo, 50), GOL_AMEACA_ARRIVE);
            c.add("Path", Comportamentos.seguirCaminho(f, caminho, 25, 30, GOL_PATRULHA_VEL), GOL_AMEACA_PATH);
        } else {
            f.debug.estado = "GOLEIRO: patrulha";
            c.add("Path", Comportamentos.seguirCaminho(f, caminho, 25, 30, GOL_PATRULHA_VEL), GOL_PATRULHA_PATH);
        }
        return c.resultado();
    }

    // ------------------------------ ATIVO ------------------------------

    private Vetor atacanteAtivo(Ficha f, Combinador c, double dt) {
        Time t = f.time;
        Bola bola = partida.bola;
        conterNoCampo(f, c);

        Vetor bolaPrev = bola.prever(0.25);
        Vetor dirGol = direcaoDoChute(t, bolaPrev);

        // Ponto "atrás" da bola, alinhado com o gol adversário (com espaço para o peteleco).
        Vetor pontoChute = bolaPrev.sub(dirGol.mul(f.raio + Bola.RAIO + ATIVO_RECUO));
        double m = f.raio + 2;
        boolean pontoValido = pontoChute.x() > m && pontoChute.x() < Campo.LARGURA - m
                && pontoChute.y() > m && pontoChute.y() < Campo.ALTURA - m;

        Vetor paraBola = bolaPrev.sub(f.pos);
        boolean alinhado = paraBola.normalizado().dot(dirGol) > ATIVO_ALINHAMENTO
                && paraBola.tamanho() < 140;

        // Disputa: adversário colado na bola, ou demorando demais para contornar.
        // Aí não adianta tentar ficar "atrás" da bola: a ficha vai direto nela.
        boolean disputa = adversarioPerto(f, bola.pos, ATIVO_RAIO_DISPUTA)
                || f.tempoPosicionando > ATIVO_TEMPO_MAX_POSICIONAR;
        if (disputa) {
            f.tempoPosicionando = 0;
            boolean naoEhContra = paraBola.normalizado().dot(dirGol) > -0.2;
            if (paraBola.tamanho() < ATIVO_DIST_PETELECO && f.recargaChute <= 0 && naoEhContra) {
                f.debug.estado = "ATIVO: disputa (peteleco)";
                darPeteleco(f, bola, ATIVO_FORCA_PETELECO * 0.85);
                return Vetor.ZERO;
            }
            f.debug.estado = "ATIVO: disputa";
            c.add("OA", Comportamentos.evitarObstaculos(f, partida.obstaculos(false), OA_CAIXA_ATIVO), ATIVO_OA_CHUTE);
            c.add("Seek", Comportamentos.seek(f, bola.pos), ATIVO_SEEK_CHUTE);
            return c.resultado();
        }

        if (alinhado && pontoValido && paraBola.tamanho() < ATIVO_DIST_PETELECO && f.recargaChute <= 0) {
            // Chute = peteleco da própria IA (a mecânica do futebol de ficha).
            // O steering posiciona; o peteleco finaliza.
            f.debug.estado = "ATIVO: peteleco!";
            f.tempoPosicionando = 0;
            darPeteleco(f, bola, ATIVO_FORCA_PETELECO);
            return Vetor.ZERO;
        }
        if (alinhado || !pontoValido) {
            // Chute: Seek "através" da bola + desvio leve das outras fichas.
            f.debug.estado = "ATIVO: chute";
            f.tempoPosicionando = 0;
            Vetor alvo = pontoValido ? bolaPrev.soma(dirGol.mul(30)) : bola.pos;
            c.add("OA", Comportamentos.evitarObstaculos(f, partida.obstaculos(false), OA_CAIXA_ATIVO), ATIVO_OA_CHUTE);
            c.add("Seek", Comportamentos.seek(f, alvo), ATIVO_SEEK_CHUTE);
        } else {
            // Posicionamento: contorna a bola (ela entra como obstáculo) e chega atrás dela.
            f.debug.estado = "ATIVO: posicionar";
            f.tempoPosicionando += dt;
            c.add("OA", Comportamentos.evitarObstaculos(f, partida.obstaculos(true), OA_CAIXA_PADRAO), ATIVO_OA_CONTORNAR);
            c.add("Arrive", Comportamentos.arrive(f, pontoChute, ATIVO_RAIO_POSICIONAR), ATIVO_ARRIVE_POSICIONAR);
        }
        return c.resultado();
    }

    /** Peteleco da IA: mira no centro da bola, com um pequeno erro aleatório. */
    private void darPeteleco(Ficha f, Bola bola, double forca) {
        Vetor mira = bola.prever(0.05).sub(f.pos).normalizado();
        double erro = (RND.nextDouble() * 2 - 1) * ATIVO_ERRO_MIRA;
        f.lancar(Vetor.porAngulo(mira.angulo() + erro).mul(forca), 0.8);
        f.recargaChute = ATIVO_RECARGA_PETELECO;
        partida.ouvinte.peteleco(forca, f.time);
    }

    private boolean adversarioPerto(Ficha f, Vetor ponto, double raio) {
        for (Ficha o : partida.fichas) {
            if (o.time != f.time && o.pos.distancia(ponto) < raio) return true;
        }
        return false;
    }

    /**
     * Para onde empurrar a bola. Normalmente é o gol adversário; mas, com a
     * bola colada numa parede, não dá para ficar "atrás" dela nessa direção,
     * então a ficha empurra a bola AO LONGO da parede (e um pouco para dentro).
     */
    private static Vetor direcaoDoChute(Time t, Vetor bola) {
        double margem = 32;
        double x = bola.x(), y = bola.y();
        if (y < margem || y > Campo.ALTURA - margem) {
            double paraDentro = y < margem ? 0.35 : -0.35;
            return Vetor.de(t.sentido, paraDentro).normalizado();
        }
        boolean naLinhaDeFundo = (x < margem || x > Campo.LARGURA - margem) && !Campo.naBocaDoGol(y);
        if (naLinhaDeFundo) {
            double paraDentroX = x < margem ? 0.5 : -0.5;
            double paraOCentro = y < Campo.CENTRO_Y ? 1 : -1;
            boolean fundoDoAdversario = Math.abs(x - t.linhaGolAdversario()) < margem;
            // No fundo do adversário, leva a bola para a boca do gol; no próprio fundo, afasta do gol.
            double sentidoY = fundoDoAdversario ? paraOCentro : -paraOCentro;
            return Vetor.de(paraDentroX, sentidoY).normalizado();
        }
        Vetor golAlvo = Campo.centroDoGol(t.linhaGolAdversario());
        return golAlvo.sub(bola).normalizado();
    }

    // ------------------------------ ZAGUEIRO ------------------------------

    private Vetor zagueiro(Ficha f, Combinador c) {
        Time t = f.time;
        conterNoCampo(f, c);
        fugirDeFichasLancadas(f, c);
        c.add("OA", Comportamentos.evitarObstaculos(f, partida.obstaculos(false), OA_CAIXA_PADRAO), ZAG_OA);

        // Cobertura: entre a bola e o próprio gol, deslocado para o lado do zagueiro.
        Vetor gol = Campo.centroDoGol(t.linhaGolProprio());
        Vetor bola = partida.bola.prever(0.3);
        Vetor dir = bola.sub(gol).normalizado();
        double d = Math.min(bola.distancia(gol) * 0.5, 330);
        double lado = f.indice == 0 ? -45 : 45;
        Vetor cobertura = gol.soma(dir.mul(d)).soma(Vetor.de(0, lado));

        double meio = Campo.LARGURA / 2 - t.sentido * 30;
        double minimoArea = t.linhaGolProprio() + t.sentido * (Campo.PEQUENA_AREA_PROF + 40);
        double x = t.sentido > 0
                ? Math.max(minimoArea, Math.min(meio, cobertura.x()))
                : Math.min(minimoArea, Math.max(meio, cobertura.x()));
        double y = Math.max(40, Math.min(Campo.ALTURA - 40, cobertura.y()));

        f.debug.estado = "ZAGUEIRO: cobertura";
        c.add("Arrive", Comportamentos.arrive(f, Vetor.de(x, y), ZAG_RAIO_DESACELERACAO), ZAG_ARRIVE_COBERTURA);
        return c.resultado();
    }

    // ------------------------------ ATACANTE SEM BOLA ------------------------------

    private Vetor atacanteSemBola(Ficha f, Combinador c, double dt) {
        Time t = f.time;
        Bola bola = partida.bola;
        conterNoCampo(f, c);
        fugirDeFichasLancadas(f, c);
        c.add("OA", Comportamentos.evitarObstaculos(f, partida.obstaculos(false), OA_CAIXA_PADRAO), APOIO_OA);

        boolean bolaNoAtaque = t.sentido * (bola.pos.x() - Campo.LARGURA / 2) > 0;
        if (bolaNoAtaque) {
            // Apoio: fica à frente da bola, aberto pelo seu lado do campo.
            f.debug.estado = "ATACANTE: apoio";
            double y = f.indice == 0 ? bola.pos.y() - 110 : bola.pos.y() + 110;
            double x = bola.pos.x() + t.sentido * 110;
            Vetor apoio = Vetor.de(
                    Math.max(60, Math.min(Campo.LARGURA - 60, x)),
                    Math.max(40, Math.min(Campo.ALTURA - 40, y)));
            c.add("Arrive", Comportamentos.arrive(f, apoio, 90), APOIO_ARRIVE);
        } else {
            // Espera: passeia (Wander) perto da sua zona; a "coleira" (Arrive) só puxa
            // de volta quando ele se afasta demais.
            f.debug.estado = "ATACANTE: espera";
            if (f.pos.distancia(f.zonaBase) > ESPERA_RAIO_COLEIRA) {
                c.add("Coleira", Comportamentos.arrive(f, f.zonaBase, 120, WANDER_VEL * 1.3), ESPERA_COLEIRA);
            }
            c.add("Wander", Comportamentos.wander(f, dt, WANDER_DISTANCIA, WANDER_RAIO, WANDER_JITTER, WANDER_VEL), ESPERA_WANDER);
        }
        return c.resultado();
    }

    // ------------------------------ AUXILIARES ------------------------------

    private static void conterNoCampo(Ficha f, Combinador c) {
        double m = f.raio + 8;
        c.add("Conter", Comportamentos.conter(f, m, m, Campo.LARGURA - m, Campo.ALTURA - m), CONTER);
    }

    /** Flee da ficha lançada (peteleco) mais próxima, se houver alguma por perto. */
    private void fugirDeFichasLancadas(Ficha f, Combinador c) {
        Ficha ameaca = null;
        double menor = Double.MAX_VALUE;
        for (Ficha o : partida.fichas) {
            if (o == f || !o.estaLivre() || o.velocidadeEscalar() < VEL_AMEACA) continue;
            double d = o.pos.distancia(f.pos);
            if (d < menor) { menor = d; ameaca = o; }
        }
        if (ameaca != null) {
            c.add("Flee", Comportamentos.flee(f, ameaca.pos, RAIO_PANICO), FLEE_FICHA_LANCADA);
        }
    }
}
