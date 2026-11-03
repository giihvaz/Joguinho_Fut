package futebol.motor;

/**
 * Agente autônomo com física de steering behaviors (modelo de Reynolds):
 *
 * <pre>
 *   força      = truncar(steering, maxForca)
 *   aceleração = força / massa
 *   velocidade = truncar(velocidade + aceleração * dt, maxVelocidade)
 *   posição    = posição + velocidade * dt
 * </pre>
 *
 * É o mesmo modelo do template da professora (Car.update). A única extensão é o
 * "modo livre": logo depois de um peteleco (ou de uma pancada forte) a ficha
 * desliza sem steering, freando só pelo atrito com a mesa, como uma ficha real.
 */
public class Agente {

    /** Atrito da mesa (1/s) aplicado no modo livre. */
    public static final double ATRITO_LIVRE = 1.4;

    public Vetor pos;
    public Vetor vel = Vetor.ZERO;

    public final double massa;
    public final double maxForca;
    public final double maxVelocidade;
    public final double raio;

    /** Direção para onde o agente "olha" (mantida mesmo parado). */
    public Vetor direcao = Vetor.de(1, 0);

    /** Estado do Wander (ângulo sobre o círculo, relativo à direção). */
    public double wanderAngulo = 0;

    /** Tempo restante de deslizamento livre (sem steering). */
    public double tempoLivre = 0;

    /** Última força aplicada, já truncada (para as setas de debug). */
    public Vetor ultimoSteering = Vetor.ZERO;

    /** Informações de debug preenchidas pelos comportamentos a cada quadro. */
    public final Debug debug = new Debug();

    public Agente(Vetor pos, double massa, double maxForca, double maxVelocidade, double raio) {
        this.pos = pos;
        this.massa = massa;
        this.maxForca = maxForca;
        this.maxVelocidade = maxVelocidade;
        this.raio = raio;
    }

    public double velocidadeEscalar() { return vel.tamanho(); }

    public boolean estaLivre() { return tempoLivre > 0; }

    /** Coloca o agente em deslizamento livre com a velocidade dada. */
    public void lancar(Vetor velocidade, double duracao) {
        vel = velocidade;
        tempoLivre = duracao;
    }

    /** Integra um passo de simulação. */
    public void atualizar(Vetor steering, double dt) {
        if (estaLivre()) {
            tempoLivre -= dt;
            vel = vel.mul(Math.exp(-ATRITO_LIVRE * dt));
            // Quando já está lento o bastante, a IA volta a controlar.
            if (vel.tamanho() < maxVelocidade * 0.6) tempoLivre = 0;
            ultimoSteering = Vetor.ZERO;
        } else {
            Vetor forca = (steering == null || !steering.valido()) ? Vetor.ZERO : steering.truncado(maxForca);
            ultimoSteering = forca;
            Vetor aceleracao = forca.div(massa);
            vel = vel.soma(aceleracao.mul(dt)).truncado(maxVelocidade);
        }
        pos = pos.soma(vel.mul(dt));
        if (vel.tamanho2() > 4) direcao = vel.normalizado();
    }

    /** Dados que os comportamentos registram para serem desenhados no modo debug. */
    public static final class Debug {
        public Vetor alvo;
        public double raioDesaceleracao;
        public Vetor panicoOrigem;
        public double panicoRaio;
        public Vetor wanderCentro;
        public double wanderRaio;
        public Vetor wanderAlvo;
        public double caixaComprimento;
        public Obstaculo obstaculoDetectado;
        public Vetor caminhoFuturo;
        public Vetor caminhoProjecao;
        public Vetor caminhoAlvo;
        public String estado = "";

        public void limpar() {
            alvo = null;
            raioDesaceleracao = 0;
            panicoOrigem = null;
            panicoRaio = 0;
            wanderCentro = null;
            wanderAlvo = null;
            caixaComprimento = 0;
            obstaculoDetectado = null;
            caminhoFuturo = null;
            caminhoProjecao = null;
            caminhoAlvo = null;
        }
    }
}
