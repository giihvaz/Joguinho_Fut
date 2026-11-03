package futebol.ia;

import futebol.motor.Vetor;

import java.util.ArrayList;
import java.util.List;

/**
 * Combina várias forças de steering por SOMA PONDERADA COM PRIORIDADE
 * ("weighted truncated running sum with prioritization", Buckland).
 *
 * <p>As forças são adicionadas da mais importante para a menos importante.
 * Cada força é multiplicada pelo seu peso e só ocupa o "orçamento" de força
 * que ainda sobra (maxForca menos o que já foi acumulado). Assim, quando há
 * um perigo (ex.: obstáculo à frente), a força de desvio consome o orçamento
 * primeiro e os comportamentos menos importantes não conseguem anulá-la.
 * Quando não há perigo, a força de desvio é zero e o orçamento fica todo para
 * os demais comportamentos.</p>
 */
public final class Combinador {

    private final double maxForca;
    private Vetor total = Vetor.ZERO;
    private final List<String> parcelas = new ArrayList<>();

    public Combinador(double maxForca) {
        this.maxForca = maxForca;
    }

    /** Adiciona uma força (na ordem de prioridade) com o seu peso. */
    public Combinador add(String nome, Vetor forca, double peso) {
        if (forca == null || forca.ehZero() || peso == 0) return this;
        double restante = maxForca - total.tamanho();
        if (restante <= 0) return this;
        Vetor ponderada = forca.mul(peso).truncado(restante);
        total = total.soma(ponderada);
        parcelas.add(nome);
        return this;
    }

    public Vetor resultado() {
        return total;
    }

    /** Nomes das forças que contribuíram neste quadro (para o HUD/debug). */
    public String descricao() {
        return String.join(" + ", parcelas);
    }
}
