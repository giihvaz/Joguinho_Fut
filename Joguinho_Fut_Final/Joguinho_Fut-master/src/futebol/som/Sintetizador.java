package futebol.som;

import java.util.Random;

/**
 * Gera os sons do jogo por síntese (não depende de arquivos externos).
 * Cada método devolve amostras em [-1, 1] a {@link #TAXA} Hz.
 *
 * <p>Técnicas usadas: ruído branco filtrado (filtros biquad passa-banda/passa-baixa),
 * senoides amortecidas (impactos) e envelopes de amplitude.</p>
 */
public final class Sintetizador {
    private Sintetizador() { }

    public static final int TAXA = 22050;

    // ------------------------------------------------------------------
    // Torcida (ambiente, em loop)
    // ------------------------------------------------------------------

    /** Murmúrio de torcida: várias "vozes" de ruído com formantes, entrando e saindo. Sem emenda no loop. */
    public static float[] torcida(double segundos, long semente) {
        Random rnd = new Random(semente);
        int n = (int) (segundos * TAXA);
        int extra = (int) (0.6 * TAXA);                // trecho usado para o crossfade do loop
        float[] s = new float[n + extra];

        // Camada 1: "chiado" de fundo da multidão (ruído rosado em banda média)
        Biquad bp1 = Biquad.passaBanda(450, 0.6), bp2 = Biquad.passaBanda(1300, 0.8);
        double lfoFase = rnd.nextDouble() * 6.28;
        for (int i = 0; i < s.length; i++) {
            double r = rnd.nextGaussian();
            double v = bp1.processar(r) * 0.6 + bp2.processar(r) * 0.35;
            double lfo = 0.8 + 0.2 * Math.sin(2 * Math.PI * 0.21 * i / TAXA + lfoFase);
            s[i] += (float) (v * lfo * 0.5);
        }

        // Camada 2: vozes (gritos/cantos) — ruído com formante e envelope lento
        int vozes = 70;
        for (int v = 0; v < vozes; v++) {
            double formante = 280 + rnd.nextDouble() * 700;
            Biquad f1 = Biquad.passaBanda(formante, 6 + rnd.nextDouble() * 6);
            Biquad f2 = Biquad.passaBanda(formante * (2.2 + rnd.nextDouble() * 0.8), 5);
            double dur = 0.4 + rnd.nextDouble() * 1.6;
            int ini = rnd.nextInt(s.length);
            int len = (int) (dur * TAXA);
            double amp = 0.25 + rnd.nextDouble() * 0.6;
            for (int k = 0; k < len; k++) {
                int idx = (ini + k) % s.length;
                double t = (double) k / len;
                double env = Math.sin(Math.PI * t);
                env *= env;
                double r = rnd.nextGaussian();
                s[idx] += (float) ((f1.processar(r) + 0.5 * f2.processar(r)) * env * amp);
            }
        }

        // Loop sem emenda: mistura o final extra com o começo
        float[] out = new float[n];
        System.arraycopy(s, 0, out, 0, n);
        for (int i = 0; i < extra; i++) {
            double t = (double) i / extra;
            out[i] = (float) (s[n + i] * (1 - t) + s[i] * t);
        }
        return normalizar(out, 0.55);
    }

    // ------------------------------------------------------------------
    // Impactos
    // ------------------------------------------------------------------

    /** Ficha batendo na bola: "toc" mais grave e curto. */
    public static float[] toqueBola() {
        int n = (int) (0.09 * TAXA);
        float[] s = new float[n];
        Random rnd = new Random(7);
        Biquad clique = Biquad.passaBanda(2500, 1.2);
        for (int i = 0; i < n; i++) {
            double t = (double) i / TAXA;
            double freq = 520 * (1 + 0.6 * Math.exp(-t * 60));         // leve queda de altura
            double corpo = Math.sin(2 * Math.PI * freq * t) * Math.exp(-t * 55)
                    + 0.4 * Math.sin(2 * Math.PI * 1150 * t) * Math.exp(-t * 90);
            double ataque = clique.processar(rnd.nextGaussian()) * Math.exp(-t * 900);
            s[i] = (float) (corpo * 0.8 + ataque * 0.6);
        }
        return normalizar(s, 0.9);
    }

    /** Ficha batendo em ficha: "clac" de plástico, mais agudo e seco. */
    public static float[] choqueFichas() {
        int n = (int) (0.06 * TAXA);
        float[] s = new float[n];
        Random rnd = new Random(11);
        Biquad clique = Biquad.passaBanda(4200, 1.5);
        for (int i = 0; i < n; i++) {
            double t = (double) i / TAXA;
            double modos = Math.sin(2 * Math.PI * 1850 * t) * Math.exp(-t * 120)
                    + 0.7 * Math.sin(2 * Math.PI * 3100 * t) * Math.exp(-t * 160)
                    + 0.4 * Math.sin(2 * Math.PI * 4700 * t) * Math.exp(-t * 220);
            double ataque = clique.processar(rnd.nextGaussian()) * Math.exp(-t * 700);
            s[i] = (float) (modos * 0.6 + ataque);
        }
        return normalizar(s, 0.9);
    }

    /**
     * Peteleco: o "tlec" do dedo (unha) batendo na borda da ficha.
     * Um estalo de ruído agudo bem curto + uma ressonância de plástico oco
     * + um leve "fff" de deslizamento no feltro logo depois.
     */
    public static float[] peteleco() {
        int n = (int) (0.16 * TAXA);
        float[] s = new float[n];
        Random rnd = new Random(23);
        Biquad estalo = Biquad.passaBanda(5200, 1.8);
        Biquad feltro = Biquad.passaBanda(1600, 0.7);
        for (int i = 0; i < n; i++) {
            double t = (double) i / TAXA;
            double unha = estalo.processar(rnd.nextGaussian()) * Math.exp(-t * 1100);
            double plastico = Math.sin(2 * Math.PI * 1350 * t) * Math.exp(-t * 85)
                    + 0.5 * Math.sin(2 * Math.PI * 2650 * t) * Math.exp(-t * 130);
            double deslize = feltro.processar(rnd.nextGaussian())
                    * (t < 0.01 ? 0 : Math.exp(-(t - 0.01) * 22)) * 0.25;
            s[i] = (float) (unha * 1.4 + plastico * 0.45 + deslize);
        }
        return normalizar(s, 0.9);
    }

    // ------------------------------------------------------------------
    // Gol
    // ------------------------------------------------------------------

    /** Explosão da torcida + buzina de estádio. */
    public static float[] gol() {
        double dur = 3.6;
        int n = (int) (dur * TAXA);
        float[] s = new float[n];
        Random rnd = new Random(3);

        // Grito da torcida: ruído largo com envelope (sobe rápido, sustenta, cai)
        Biquad a = Biquad.passaBanda(700, 0.5), b = Biquad.passaBanda(1900, 0.7), c = Biquad.passaBanda(3500, 1.0);
        for (int i = 0; i < n; i++) {
            double t = (double) i / TAXA;
            double env = t < 0.25 ? t / 0.25 : t < 1.9 ? 1.0 : Math.max(0, 1 - (t - 1.9) / (dur - 1.9));
            env *= 0.85 + 0.15 * Math.sin(2 * Math.PI * 3.2 * t);   // "ondas" do grito
            double r = rnd.nextGaussian();
            s[i] = (float) ((a.processar(r) + 0.7 * b.processar(r) + 0.25 * c.processar(r)) * env);
        }

        // Vozes individuais por cima ("GOOOL")
        for (int v = 0; v < 40; v++) {
            double f0 = 180 + rnd.nextDouble() * 260;
            double ini = 0.1 + rnd.nextDouble() * 0.4, fim = 1.6 + rnd.nextDouble() * 1.4;
            double fase = rnd.nextDouble() * 6.28;
            double vib = 4 + rnd.nextDouble() * 3;
            Biquad form = Biquad.passaBanda(750 + rnd.nextDouble() * 300, 3);
            double ph = 0;
            for (int i = (int) (ini * TAXA); i < Math.min(n, (int) (fim * TAXA)); i++) {
                double t = (double) i / TAXA;
                double u = (t - ini) / (fim - ini);
                double env = Math.min(1, u * 8) * Math.min(1, (1 - u) * 3);
                double f = f0 * (1 + 0.02 * Math.sin(2 * Math.PI * vib * t + fase));
                ph += 2 * Math.PI * f / TAXA;
                double serra = 0;
                for (int h = 1; h <= 8; h++) serra += Math.sin(h * ph) / h;
                s[i] += (float) (form.processar(serra) * env * 0.12);
            }
        }

        // Buzina de estádio (duas notas)
        double[][] notas = {{0.15, 0.75, 311.1}, {0.8, 1.7, 370.0}};
        Biquad pb = Biquad.passaBaixa(1800, 0.7);
        for (double[] nota : notas) {
            double ph = 0;
            for (int i = (int) (nota[0] * TAXA); i < (int) (nota[1] * TAXA); i++) {
                double t = (double) i / TAXA;
                double u = (t - nota[0]) / (nota[1] - nota[0]);
                double env = Math.min(1, u * 15) * Math.min(1, (1 - u) * 10);
                ph += 2 * Math.PI * nota[2] / TAXA;
                double onda = 0;
                for (int h = 1; h <= 10; h++) onda += Math.sin(h * ph) / h;
                s[i] += (float) (pb.processar(onda) * env * 0.45);
            }
        }
        return normalizar(s, 0.95);
    }

    // ------------------------------------------------------------------
    // Roleta
    // ------------------------------------------------------------------

    /** "Tique" da roleta passando por cada seleção. */
    public static float[] tique() {
        int n = (int) (0.025 * TAXA);
        float[] s = new float[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / TAXA;
            s[i] = (float) (Math.sin(2 * Math.PI * 2900 * t) * Math.exp(-t * 260));
        }
        return normalizar(s, 0.8);
    }

    /** "Ding" quando a roleta para. */
    public static float[] revelacao() {
        int n = (int) (1.2 * TAXA);
        float[] s = new float[n];
        double[] freqs = {880, 1108.7, 1318.5};
        for (int i = 0; i < n; i++) {
            double t = (double) i / TAXA;
            double v = 0;
            for (int k = 0; k < freqs.length; k++) {
                double atraso = k * 0.07;
                if (t < atraso) continue;
                double tt = t - atraso;
                v += Math.sin(2 * Math.PI * freqs[k] * tt) * Math.exp(-tt * 3.5)
                        + 0.3 * Math.sin(2 * Math.PI * freqs[k] * 2 * tt) * Math.exp(-tt * 6);
            }
            s[i] = (float) v;
        }
        return normalizar(s, 0.7);
    }

    // ------------------------------------------------------------------

    static float[] normalizar(float[] s, double pico) {
        double max = 1e-9;
        for (float v : s) max = Math.max(max, Math.abs(v));
        double k = pico / max;
        for (int i = 0; i < s.length; i++) s[i] = (float) (s[i] * k);
        return s;
    }

    /** Converte para PCM 16 bits little-endian. */
    public static byte[] paraPcm16(float[] s) {
        byte[] b = new byte[s.length * 2];
        for (int i = 0; i < s.length; i++) {
            int v = (int) Math.round(Math.max(-1, Math.min(1, s[i])) * 32767);
            b[2 * i] = (byte) v;
            b[2 * i + 1] = (byte) (v >> 8);
        }
        return b;
    }

    /** Filtro biquad (fórmulas do "Audio EQ Cookbook" de R. Bristow-Johnson). */
    static final class Biquad {
        private final double b0, b1, b2, a1, a2;
        private double x1, x2, y1, y2;

        private Biquad(double b0, double b1, double b2, double a0, double a1, double a2) {
            this.b0 = b0 / a0; this.b1 = b1 / a0; this.b2 = b2 / a0;
            this.a1 = a1 / a0; this.a2 = a2 / a0;
        }

        static Biquad passaBanda(double f, double q) {
            double w = 2 * Math.PI * f / TAXA, alfa = Math.sin(w) / (2 * q);
            return new Biquad(alfa, 0, -alfa, 1 + alfa, -2 * Math.cos(w), 1 - alfa);
        }

        static Biquad passaBaixa(double f, double q) {
            double w = 2 * Math.PI * f / TAXA, alfa = Math.sin(w) / (2 * q), cw = Math.cos(w);
            return new Biquad((1 - cw) / 2, 1 - cw, (1 - cw) / 2, 1 + alfa, -2 * cw, 1 - alfa);
        }

        double processar(double x) {
            double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1; x1 = x; y2 = y1; y1 = y;
            return y;
        }
    }
}
