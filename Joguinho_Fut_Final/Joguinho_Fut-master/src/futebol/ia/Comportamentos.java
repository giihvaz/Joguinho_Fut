package futebol.ia;

import futebol.motor.Agente;
import futebol.motor.Caminho;
import futebol.motor.Obstaculo;
import futebol.motor.Vetor;

import java.util.List;
import java.util.Random;

/**
 * Os steering behaviors. Cada método devolve uma FORÇA de steering (não uma
 * velocidade). Quem soma, pondera e trunca as forças é o {@link Combinador}
 * e, no final, o {@link Agente#atualizar}.
 */
public final class Comportamentos {

    private Comportamentos() { }

    private static final Random RND = new Random();

    // ------------------------------------------------------------------
    // SEEK — ir em direção a um alvo
    // ------------------------------------------------------------------

    /**
     * velocidadeDesejada = normalizar(alvo - posição) * maxVelocidade
     * steering           = velocidadeDesejada - velocidadeAtual
     */
    public static Vetor seek(Agente a, Vetor alvo) {
        return seek(a, alvo, a.maxVelocidade);
    }

    public static Vetor seek(Agente a, Vetor alvo, double velocidade) {
        a.debug.alvo = alvo;
        Vetor desejada = alvo.sub(a.pos).comTamanho(velocidade);
        return desejada.sub(a.vel);
    }

    // ------------------------------------------------------------------
    // FLEE — fugir de uma ameaça (somente dentro do raio de pânico)
    // ------------------------------------------------------------------

    /**
     * Inverso do Seek: a velocidade desejada aponta para longe da ameaça.
     * Fora do raio de pânico a força é zero, para o agente não fugir de algo
     * que está do outro lado do campo.
     */
    public static Vetor flee(Agente a, Vetor ameaca, double raioPanico) {
        a.debug.panicoOrigem = ameaca;
        a.debug.panicoRaio = raioPanico;
        if (a.pos.distancia(ameaca) > raioPanico) return Vetor.ZERO;
        Vetor desejada = a.pos.sub(ameaca).comTamanho(a.maxVelocidade);
        return desejada.sub(a.vel);
    }

    // ------------------------------------------------------------------
    // ARRIVE — chegar a um alvo desacelerando
    // ------------------------------------------------------------------

    /** Tempo de reação do Arrive (Millington: "time to target"). */
    public static final double ARRIVE_TEMPO_REACAO = 0.25;

    /**
     * Fora do raio de desaceleração age como o Seek. Dentro dele, a
     * velocidade desejada cai linearmente até zero no alvo:
     * <pre>
     *   velDesejada = maxVel * (distância / raioDesaceleracao)
     *   steering    = (desejada - atual) / tempoReacao
     * </pre>
     * A divisão pelo tempo de reação faz a ficha corrigir o erro de velocidade
     * em ~0,25 s; sem ela a frenagem final fica lenta e "boiando".
     * Para não passar do alvo, o raio precisa ser maior que a distância de
     * frenagem: maxVel² / (2 * maxForca / massa).
     */
    public static Vetor arrive(Agente a, Vetor alvo, double raioDesaceleracao) {
        return arrive(a, alvo, raioDesaceleracao, a.maxVelocidade);
    }

    public static Vetor arrive(Agente a, Vetor alvo, double raioDesaceleracao, double velMax) {
        a.debug.alvo = alvo;
        a.debug.raioDesaceleracao = raioDesaceleracao;
        Vetor paraAlvo = alvo.sub(a.pos);
        double d = paraAlvo.tamanho();
        double velDesejada = d >= raioDesaceleracao ? velMax : velMax * d / raioDesaceleracao;
        Vetor desejada = paraAlvo.comTamanho(velDesejada);
        return desejada.sub(a.vel).div(ARRIVE_TEMPO_REACAO);
    }

    // ------------------------------------------------------------------
    // WANDER — passear de forma aleatória, mas suave
    // ------------------------------------------------------------------

    /**
     * Um círculo é projetado à frente do agente. Um ponto sobre esse círculo
     * passeia aleatoriamente (o ângulo sofre pequenas variações a cada quadro)
     * e o agente faz Seek nesse ponto. Como o ponto só se move um pouco por
     * quadro, a trajetória fica sinuosa e contínua, sem tremer.
     *
     * A variação do ângulo é proporcional a sqrt(dt) (passeio aleatório), assim
     * o comportamento não muda com a taxa de quadros.
     */
    public static Vetor wander(Agente a, double dt, double distancia, double raio, double jitter, double velocidade) {
        a.wanderAngulo += RND.nextGaussian() * jitter * Math.sqrt(dt);
        // Mantém o ângulo em [-PI, PI] só por higiene numérica.
        a.wanderAngulo = Math.atan2(Math.sin(a.wanderAngulo), Math.cos(a.wanderAngulo));

        Vetor centro = a.pos.soma(a.direcao.mul(distancia));
        Vetor alvo = centro.soma(Vetor.porAngulo(a.direcao.angulo() + a.wanderAngulo).mul(raio));

        a.debug.wanderCentro = centro;
        a.debug.wanderRaio = raio;
        a.debug.wanderAlvo = alvo;
        Vetor desejada = alvo.sub(a.pos).comTamanho(velocidade);
        return desejada.sub(a.vel);
    }

    // ------------------------------------------------------------------
    // OBSTACLE AVOIDANCE — desviar de obstáculos circulares
    // ------------------------------------------------------------------

    /** Folga extra (px) somada ao raio dos obstáculos. */
    public static final double OA_FOLGA = 6;

    /**
     * Uma "caixa de detecção" se estende à frente do agente, com a largura do
     * agente e comprimento proporcional à velocidade. Cada obstáculo é levado
     * para o espaço local do agente (x = para frente, y = para o lado):
     * <ul>
     *   <li>atrás do agente (x &lt; 0) ou além da caixa: ignorado;</li>
     *   <li>|y| maior que (raioObstáculo + raioAgente): não cruza o caminho;</li>
     *   <li>dos que sobram, o de interseção mais próxima é o mais perigoso.</li>
     * </ul>
     * A resposta é uma força lateral (para o lado oposto ao obstáculo) somada
     * a uma leve frenagem. As duas crescem com a urgência (quanto mais perto,
     * mais forte) e a força lateral também cresce com a sobreposição (quanto
     * mais centralizado o obstáculo, mais forte).
     */
    public static Vetor evitarObstaculos(Agente a, List<Obstaculo> obstaculos, double comprimentoMinimo) {
        double comprimento = comprimentoMinimo + (a.velocidadeEscalar() / a.maxVelocidade) * comprimentoMinimo;
        a.debug.caixaComprimento = comprimento;

        Vetor frente = a.direcao;
        Vetor lado = frente.perpendicular();

        Obstaculo maisPerigoso = null;
        double menorInterseccao = Double.MAX_VALUE;
        double yLocal = 0, raioExpandido = 0;

        for (Obstaculo o : obstaculos) {
            if (o.dono() == a) continue;
            Vetor rel = o.centro().sub(a.pos);
            double lx = rel.dot(frente);
            double ly = rel.dot(lado);
            double r = o.raio() + a.raio + OA_FOLGA;

            if (lx + r < 0) continue;              // totalmente atrás
            if (lx - r > comprimento) continue;    // além da caixa
            if (Math.abs(ly) >= r) continue;       // não cruza a trajetória

            // Interseção do círculo expandido com o eixo x local.
            double s = Math.sqrt(r * r - ly * ly);
            double ip = lx - s;
            if (ip <= 0) ip = lx + s;
            if (ip <= 0) continue;

            if (ip < menorInterseccao) {
                menorInterseccao = ip;
                maisPerigoso = o;
                yLocal = ly;
                raioExpandido = r;
            }
        }

        a.debug.obstaculoDetectado = maisPerigoso;
        if (maisPerigoso == null) return Vetor.ZERO;

        double urgencia = clamp(1 - menorInterseccao / comprimento, 0, 1);
        double sobreposicao = 1 - Math.abs(yLocal) / raioExpandido;
        double sinal = yLocal >= 0 ? -1 : 1;  // empurra para o lado oposto

        double lateral = sinal * a.maxForca * (0.35 + 0.65 * urgencia) * (0.5 + 0.5 * sobreposicao);
        double frenagem = -a.maxForca * 0.4 * urgencia;

        return frente.mul(frenagem).soma(lado.mul(lateral));
    }

    // ------------------------------------------------------------------
    // PATH FOLLOWING — seguir um caminho (Reynolds)
    // ------------------------------------------------------------------

    /**
     * 1. Prevê onde o agente estará daqui a pouco: futura = pos + dir * previsao.
     * 2. Projeta essa posição no caminho (ponto mais próximo — "normal").
     * 3. Anda {@code avanco} px adiante no caminho a partir da normal: esse é o alvo.
     * 4. Se a posição futura sai do corredor (distância &gt; raio do caminho),
     *    faz Seek no alvo; se continua dentro, não aplica força (segue reto).
     *    Exceção: se estiver lento, sempre faz Seek, para não ficar parado.
     */
    public static Vetor seguirCaminho(Agente a, Caminho caminho, double previsao, double avanco) {
        return seguirCaminho(a, caminho, previsao, avanco, a.maxVelocidade);
    }

    public static Vetor seguirCaminho(Agente a, Caminho caminho, double previsao, double avanco, double velocidade) {
        Vetor futura = a.pos.soma(a.vel.comTamanho(previsao));
        Caminho.Projecao p = caminho.projetar(futura);
        Vetor alvo = caminho.avancar(p.segmento(), p.ponto(), avanco);

        a.debug.caminhoFuturo = futura;
        a.debug.caminhoProjecao = p.ponto();
        a.debug.caminhoAlvo = alvo;

        boolean foraDoCorredor = p.distancia() > caminho.raio;
        boolean lento = a.velocidadeEscalar() < velocidade * 0.5;
        if (foraDoCorredor || lento) {
            Vetor desejada = alvo.sub(a.pos).comTamanho(velocidade);
            return desejada.sub(a.vel);
        }
        return Vetor.ZERO;
    }

    // ------------------------------------------------------------------
    // CONTENÇÃO — auxiliar: manter-se dentro de um retângulo
    // ------------------------------------------------------------------

    /**
     * Não é um dos comportamentos pedidos; é um auxiliar. Se a posição prevista
     * sai do retângulo, empurra o agente de volta para o centro dele.
     */
    public static Vetor conter(Agente a, double minX, double minY, double maxX, double maxY) {
        Vetor futura = a.pos.soma(a.vel.mul(0.35));
        boolean dentro = futura.x() > minX && futura.x() < maxX && futura.y() > minY && futura.y() < maxY;
        if (dentro) return Vetor.ZERO;
        Vetor centro = Vetor.de((minX + maxX) / 2, (minY + maxY) / 2);
        Vetor desejada = centro.sub(a.pos).comTamanho(a.maxVelocidade);
        return desejada.sub(a.vel);
    }

    static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
