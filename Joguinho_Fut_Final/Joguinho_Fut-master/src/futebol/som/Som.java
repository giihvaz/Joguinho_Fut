package futebol.som;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Toca os efeitos sonoros. Os sons são sintetizados na inicialização
 * ({@link Sintetizador}); se existir um arquivo {@code src/sons/<nome>.wav},
 * ele é usado no lugar (dá para trocar por gravações reais sem mexer no código).
 *
 * <p>Se o computador não tiver saída de áudio, tudo vira silêncio sem erro.</p>
 */
public final class Som {

    public enum Efeito {
        TORCIDA("torcida", 1),
        BOLA("bola", 4),
        FICHAS("fichas", 6),
        GOL("gol", 1),
        PETELECO("peteleco", 3),
        TIQUE("tique", 4),
        REVELACAO("revelacao", 1);

        final String arquivo;
        final int copias;

        Efeito(String arquivo, int copias) {
            this.arquivo = arquivo;
            this.copias = copias;
        }
    }

    /** Intervalo mínimo entre dois disparos do mesmo efeito (evita "metralhadora" de cliques). */
    private static final long INTERVALO_MIN_NS = 45_000_000L;
    private static final float VOLUME_TORCIDA = 0.45f;

    private final Map<Efeito, Clip[]> clips = new EnumMap<>(Efeito.class);
    private final Map<Efeito, Integer> proximo = new EnumMap<>(Efeito.class);
    private final Map<Efeito, Long> ultimoDisparo = new EnumMap<>(Efeito.class);

    private boolean disponivel = false;
    private boolean ligado = true;
    private boolean torcidaDesejada = false;

    public Som() {
        try {
            AudioFormat formato = new AudioFormat(Sintetizador.TAXA, 16, 1, true, false);
            for (Efeito e : Efeito.values()) {
                Clip[] pool = new Clip[e.copias];
                byte[] pcm = null;
                for (int i = 0; i < e.copias; i++) {
                    Clip c = AudioSystem.getClip();
                    if (!abrirArquivo(c, e.arquivo)) {
                        if (pcm == null) pcm = Sintetizador.paraPcm16(sintetizar(e));
                        c.open(formato, pcm, 0, pcm.length);
                    }
                    pool[i] = c;
                }
                clips.put(e, pool);
                proximo.put(e, 0);
                ultimoDisparo.put(e, 0L);
            }
            disponivel = true;
        } catch (Exception | LinkageError ex) {
            System.err.println("Som desativado (sem dispositivo de áudio): " + ex.getMessage());
            disponivel = false;
        }
    }

    private static float[] sintetizar(Efeito e) {
        return switch (e) {
            case TORCIDA -> Sintetizador.torcida(8, 42);
            case BOLA -> Sintetizador.toqueBola();
            case FICHAS -> Sintetizador.choqueFichas();
            case GOL -> Sintetizador.gol();
            case PETELECO -> Sintetizador.peteleco();
            case TIQUE -> Sintetizador.tique();
            case REVELACAO -> Sintetizador.revelacao();
        };
    }

    /**
     * Tenta carregar {@code sons/<nome>.wav}: primeiro do classpath (IntelliJ copia
     * a pasta src/sons para a saída) e, se não achar, direto de {@code src/sons/}
     * (útil quando se compila pelo terminal com javac, que não copia arquivos .wav).
     */
    private static boolean abrirArquivo(Clip c, String nome) {
        InputStream in = Som.class.getResourceAsStream("/sons/" + nome + ".wav");
        if (in == null) {
            java.io.File arquivo = new java.io.File("src/sons/" + nome + ".wav");
            if (!arquivo.isFile()) return false;
            try {
                in = new java.io.FileInputStream(arquivo);
            } catch (java.io.IOException ex) {
                return false;
            }
        }
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(in))) {
            c.open(ais);
            return true;
        } catch (Exception ex) {
            System.err.println("Não consegui abrir sons/" + nome + ".wav, usando som sintetizado.");
            return false;
        }
    }

    // ------------------------------------------------------------------

    public boolean isLigado() { return ligado; }
    public boolean isDisponivel() { return disponivel; }

    public void alternar() {
        ligado = !ligado;
        if (!ligado) pararTorcida(false);
        else if (torcidaDesejada) iniciarTorcida();
    }

    /** Toca um efeito; {@code volume} em [0, 1]. */
    public void tocar(Efeito e, double volume) {
        if (!disponivel || !ligado || volume <= 0.02) return;
        long agora = System.nanoTime();
        if (e != Efeito.TIQUE && agora - ultimoDisparo.get(e) < INTERVALO_MIN_NS) return;
        ultimoDisparo.put(e, agora);

        Clip[] pool = clips.get(e);
        int i = proximo.get(e);
        proximo.put(e, (i + 1) % pool.length);
        Clip c = pool[i];
        c.stop();
        ajustarVolume(c, (float) Math.min(1, volume));
        c.setFramePosition(0);
        c.start();
    }

    public void iniciarTorcida() {
        torcidaDesejada = true;
        if (!disponivel || !ligado) return;
        Clip c = clips.get(Efeito.TORCIDA)[0];
        if (c.isRunning()) return;
        ajustarVolume(c, VOLUME_TORCIDA);
        c.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void pararTorcida() {
        pararTorcida(true);
    }

    private void pararTorcida(boolean esquecer) {
        if (esquecer) torcidaDesejada = false;
        if (!disponivel) return;
        clips.get(Efeito.TORCIDA)[0].stop();
    }

    private static void ajustarVolume(Clip c, float volume) {
        if (!c.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl ganho = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
        float db = (float) (20 * Math.log10(Math.max(volume, 1e-4)));
        ganho.setValue(Math.max(ganho.getMinimum(), Math.min(ganho.getMaximum(), db)));
    }
}
