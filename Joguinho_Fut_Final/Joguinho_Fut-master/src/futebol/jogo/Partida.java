package futebol.jogo;

import futebol.motor.Obstaculo;
import futebol.motor.Vetor;
import futebol.selecoes.Selecao;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Estado e regras da partida: fichas, bola, colisões, gols e o peteleco do jogador. */
public class Partida {

    public static final double FORCA_MAX_PETELECO = 1000;   // px/s
    public static final double PUXADA_MAXIMA = 120;         // px de arrasto para força máxima
    public static final double DURACAO_LIVRE_PETELECO = 1.5;
    public static final double PAUSA_GOL = 1.8;

    public final List<Ficha> fichas = new ArrayList<>();
    public final Bola bola = new Bola();
    public final Tatica tatica;

    public int golsBrasil = 0;
    public int golsAdversario = 0;

    /** Seleção sorteada na roleta. */
    public final Selecao adversario;

    /**
     * Avisos para quem quiser reagir aos acontecimentos (o som, por exemplo).
     * O impulso indica a força do choque, para o volume acompanhar a pancada.
     */
    public interface Ouvinte {
        default void bolaTocada(double impulso) { }
        default void fichasChocaram(double impulso) { }
        default void gol(Time quemFez) { }
        /** Uma ficha recebeu um peteleco (do jogador ou da IA). */
        default void peteleco(double velocidade, Time time) { }
    }

    public Ouvinte ouvinte = new Ouvinte() { };
    public String mensagem = "Arraste uma ficha do Brasil para trás e solte para dar o peteleco!";
    public double tempoMensagem = 6;
    public double pausaGol = 0;

    /** Regra da "bola presa": se a bola fica parada muito tempo, é reposta mais para dentro. */
    public static final double TEMPO_BOLA_PRESA = 3.5;
    private double tempoBolaParada = 0;

    /** Se falso, as fichas do Brasil só se movem por peteleco (jogo "raiz"). */
    public boolean iaBrasil = true;

    private final Map<Time, Ficha> ativos = new EnumMap<>(Time.class);

    // Peteleco em andamento
    public Ficha selecionada;
    public Vetor pontoArraste;

    public Partida(Selecao adversario) {
        this.adversario = adversario;
        criarTimes();
        this.tatica = new Tatica(this);
        reiniciar();
    }

    private void criarTimes() {
        for (Time t : Time.values()) {
            Selecao s = selecao(t);
            double cy = Campo.CENTRO_Y;
            fichas.add(new Ficha(t, s, Papel.GOLEIRO, 0, v(t, 30, cy), v(t, 30, cy)));
            fichas.add(new Ficha(t, s, Papel.ZAGUEIRO, 0, v(t, 190, cy - 110), v(t, 190, cy - 110)));
            fichas.add(new Ficha(t, s, Papel.ZAGUEIRO, 1, v(t, 190, cy + 110), v(t, 190, cy + 110)));
            fichas.add(new Ficha(t, s, Papel.ATACANTE, 0, v(t, 400, cy - 150), v(t, 560, cy - 140)));
            fichas.add(new Ficha(t, s, Papel.ATACANTE, 1, v(t, 400, cy + 150), v(t, 560, cy + 140)));
        }
    }

    public Selecao selecao(Time t) {
        return t == Time.BRASIL ? Selecao.BRASIL : adversario;
    }

    /** Cria um ponto "do ponto de vista do Brasil", espelhado para o adversário. */
    private static Vetor v(Time t, double x, double y) {
        return Vetor.de(t.espelharX(x), y);
    }

    public void reiniciar() {
        fichas.forEach(Ficha::voltarParaFormacao);
        bola.pos = Campo.CENTRO;
        bola.vel = Vetor.ZERO;
        ativos.clear();
        selecionada = null;
        tempoBolaParada = 0;
    }

    public void zerarPlacar() {
        golsBrasil = 0;
        golsAdversario = 0;
        pausaGol = 0;
        reiniciar();
        mostrar("Placar zerado.");
    }

    public void mostrar(String msg) {
        mensagem = msg;
        tempoMensagem = 3;
    }

    public boolean controladaPelaIA(Ficha f) {
        return f.time == Time.ADVERSARIO || iaBrasil;
    }

    public Ficha ativo(Time t) {
        return ativos.get(t);
    }

    /**
     * Lista de obstáculos vista pelas fichas: todas as fichas, as traves e,
     * opcionalmente, a bola (quando a ficha quer contorná-la sem empurrá-la).
     */
    public List<Obstaculo> obstaculos(boolean incluirBola) {
        List<Obstaculo> lista = new ArrayList<>(fichas.size() + 5);
        for (Ficha f : fichas) lista.add(new Obstaculo(f.pos, f.raio, f));
        lista.addAll(Campo.TRAVES);
        if (incluirBola) lista.add(new Obstaculo(bola.pos, Bola.RAIO, bola));
        return lista;
    }

    // ------------------------------------------------------------------
    // Laço principal
    // ------------------------------------------------------------------

    public void atualizar(double dt) {
        if (tempoMensagem > 0) tempoMensagem -= dt;

        if (pausaGol > 0) {
            pausaGol -= dt;
            if (pausaGol <= 0) reiniciar();
            return;
        }

        escolherAtivos();
        for (Ficha f : fichas) if (f.recargaChute > 0) f.recargaChute -= dt;

        // 1) Cada ficha calcula sua força com base no estado ATUAL de todos.
        List<Vetor> forcas = new ArrayList<>(fichas.size());
        for (Ficha f : fichas) {
            if (f == selecionada) {
                // Ficha sendo mirada pelo jogador: fica parada até o peteleco.
                f.vel = Vetor.ZERO;
                f.debug.limpar();
                f.debug.estado = "MIRANDO";
                forcas.add(null);
            } else if (controladaPelaIA(f) && !f.estaLivre()) {
                forcas.add(tatica.calcular(f, dt));
            } else {
                f.debug.limpar();
                f.debug.estado = f.estaLivre() ? "PETELECO (livre)" : "parada (sem IA)";
                forcas.add(null);
            }
        }

        // 2) Integra todo mundo.
        for (int i = 0; i < fichas.size(); i++) {
            Ficha f = fichas.get(i);
            if (f == selecionada) {
                f.ultimoSteering = Vetor.ZERO;
            } else if (!controladaPelaIA(f) && !f.estaLivre()) {
                // Sem IA, a ficha só desliza e para pelo atrito da mesa.
                f.vel = f.vel.mul(Math.exp(-3.0 * dt));
                if (f.vel.tamanho() < 3) f.vel = Vetor.ZERO;
                f.pos = f.pos.soma(f.vel.mul(dt));
                f.ultimoSteering = Vetor.ZERO;
            } else {
                f.atualizar(forcas.get(i), dt);
            }
        }
        bola.atualizar(dt);

        // 3) Colisões (algumas iterações para estabilizar pilhas de fichas).
        for (int it = 0; it < 3; it++) resolverColisoes();

        verificarGol();
        verificarBolaPresa(dt);
    }

    /**
     * Duas fichas (ou ficha + parede) podem prender a bola e ficar empurrando
     * para sempre. Como no futebol de mesa, a bola é reposta um pouco mais
     * para o meio do campo.
     */
    private void verificarBolaPresa(double dt) {
        if (pausaGol > 0 || bola.vel.tamanho() > 15) {
            tempoBolaParada = 0;
            return;
        }
        tempoBolaParada += dt;
        if (tempoBolaParada < TEMPO_BOLA_PRESA) return;
        tempoBolaParada = 0;
        Vetor paraCentro = Campo.CENTRO.sub(bola.pos);
        Vetor nova = bola.pos.soma(paraCentro.comTamanho(Math.min(90, paraCentro.tamanho())));
        double m = 60;
        bola.pos = Vetor.de(Math.max(m, Math.min(Campo.LARGURA - m, nova.x())),
                Math.max(m, Math.min(Campo.ALTURA - m, nova.y())));
        bola.vel = Vetor.ZERO;
        mostrar("Bola presa! Reposição mais para o meio do campo.");
    }

    /** Ativo = ficha de linha da IA mais perto da bola (com histerese para não ficar trocando). */
    private void escolherAtivos() {
        Vetor alvo = bola.prever(0.2);
        for (Time t : Time.values()) {
            Ficha atual = ativos.get(t);
            Ficha melhor = null;
            double melhorDist = Double.MAX_VALUE;
            for (Ficha f : fichas) {
                if (f.time != t || f.papel == Papel.GOLEIRO || !controladaPelaIA(f)) continue;
                double d = f.pos.distancia(alvo);
                if (f == atual) d -= 35;
                if (d < melhorDist) { melhorDist = d; melhor = f; }
            }
            ativos.put(t, melhor);
        }
    }

    private void resolverColisoes() {
        for (int i = 0; i < fichas.size(); i++) {
            for (int k = i + 1; k < fichas.size(); k++) {
                Ficha a = fichas.get(i), b = fichas.get(k);
                double impulso = Fisica.colidir(a, b, 0.5);
                if (impulso > 0) ouvinte.fichasChocaram(impulso);
                // Pancada forte: a ficha atingida "desliza" um pouco, como na mesa.
                if (impulso > 200) {
                    Ficha atingida = a.velocidadeEscalar() < b.velocidadeEscalar() ? a : b;
                    atingida.tempoLivre = Math.max(atingida.tempoLivre, 0.35);
                }
            }
        }
        for (Ficha f : fichas) {
            double toque = Fisica.colidir(f, bola, 0.8);
            if (toque > 0) ouvinte.bolaTocada(toque);
            for (Obstaculo trave : Campo.TRAVES) Fisica.colidirFixo(f, trave, 0.4);
            Fisica.conterNoRetangulo(f, 0, 0, Campo.LARGURA, Campo.ALTURA, 0.4);
        }
        for (Obstaculo trave : Campo.TRAVES) Fisica.colidirFixo(bola, trave, 0.7);
        conterBola();
    }

    /** A bola rebate nas paredes, exceto na boca do gol (onde ela pode entrar). */
    private void conterBola() {
        double r = Bola.RAIO;
        double x = bola.pos.x(), y = bola.pos.y(), vx = bola.vel.x(), vy = bola.vel.y();
        boolean boca = Campo.naBocaDoGol(y);
        if (!boca) {
            if (x < r) { x = r; if (vx < 0) vx = -vx * 0.8; }
            if (x > Campo.LARGURA - r) { x = Campo.LARGURA - r; if (vx > 0) vx = -vx * 0.8; }
        }
        if (y < r) { y = r; if (vy < 0) vy = -vy * 0.8; }
        if (y > Campo.ALTURA - r) { y = Campo.ALTURA - r; if (vy > 0) vy = -vy * 0.8; }
        bola.pos = Vetor.de(x, y);
        bola.vel = Vetor.de(vx, vy);
    }

    private void verificarGol() {
        if (bola.pos.x() < -Bola.RAIO) {
            golsAdversario++;
            gol(Time.ADVERSARIO);
        } else if (bola.pos.x() > Campo.LARGURA + Bola.RAIO) {
            golsBrasil++;
            gol(Time.BRASIL);
        }
    }

    private void gol(Time quemFez) {
        mensagem = ("GOL " + selecao(quemFez).comPreposicao() + "!").toUpperCase();
        ouvinte.gol(quemFez);
        tempoMensagem = PAUSA_GOL;
        pausaGol = PAUSA_GOL;
        bola.vel = Vetor.ZERO;
        selecionada = null;
    }

    // ------------------------------------------------------------------
    // Peteleco (entrada do jogador)
    // ------------------------------------------------------------------

    public void pressionar(Vetor p) {
        if (pausaGol > 0) return;
        Ficha melhor = null;
        double menor = Double.MAX_VALUE;
        for (Ficha f : fichas) {
            if (f.time != Time.BRASIL) continue;
            double d = f.pos.distancia(p);
            if (d < f.raio + 8 && d < menor) { menor = d; melhor = f; }
        }
        selecionada = melhor;
        pontoArraste = p;
    }

    public void arrastar(Vetor p) {
        if (selecionada != null) pontoArraste = p;
    }

    public void soltar(Vetor p) {
        if (selecionada == null) return;
        Vetor velocidade = velocidadePeteleco(selecionada, p);
        if (velocidade.tamanho() > 40) {
            selecionada.lancar(velocidade, DURACAO_LIVRE_PETELECO);
            ouvinte.peteleco(velocidade.tamanho(), Time.BRASIL);
        }
        selecionada = null;
    }

    /** Estilingue: puxa para trás, a ficha sai para o lado oposto. */
    public Vetor velocidadePeteleco(Ficha f, Vetor ponto) {
        Vetor puxada = f.pos.sub(ponto);
        double fracao = Math.min(puxada.tamanho(), PUXADA_MAXIMA) / PUXADA_MAXIMA;
        return puxada.comTamanho(fracao * FORCA_MAX_PETELECO);
    }
}
