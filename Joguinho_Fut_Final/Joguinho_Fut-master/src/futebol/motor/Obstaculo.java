package futebol.motor;

/** Obstáculo circular (fichas, traves e cones são todos tratados como círculos). */
public record Obstaculo(Vetor centro, double raio, Object dono) {
    public Obstaculo(Vetor centro, double raio) {
        this(centro, raio, null);
    }
}
