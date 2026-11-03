package futebol.motor;

/**
 * Vetor 2D imutável. Toda operação devolve um novo vetor, o que evita
 * efeitos colaterais quando o mesmo vetor é usado por vários comportamentos.
 */
public record Vetor(double x, double y) {

    public static final Vetor ZERO = new Vetor(0, 0);

    public static Vetor de(double x, double y) {
        return new Vetor(x, y);
    }

    /** Vetor unitário com o ângulo dado (radianos). */
    public static Vetor porAngulo(double angulo) {
        return new Vetor(Math.cos(angulo), Math.sin(angulo));
    }

    public Vetor soma(Vetor o) { return new Vetor(x + o.x, y + o.y); }
    public Vetor sub(Vetor o) { return new Vetor(x - o.x, y - o.y); }
    public Vetor mul(double k) { return new Vetor(x * k, y * k); }
    public Vetor div(double k) { return new Vetor(x / k, y / k); }
    public Vetor negativo() { return new Vetor(-x, -y); }

    public double dot(Vetor o) { return x * o.x + y * o.y; }
    public double tamanho2() { return x * x + y * y; }
    public double tamanho() { return Math.sqrt(tamanho2()); }
    public boolean ehZero() { return tamanho2() < 1e-12; }

    public double distancia(Vetor o) { return sub(o).tamanho(); }

    /** Vetor unitário na mesma direção (ou zero, se o vetor for zero). */
    public Vetor normalizado() {
        double t = tamanho();
        return t < 1e-9 ? ZERO : div(t);
    }

    /** Limita o tamanho do vetor a {@code max}. */
    public Vetor truncado(double max) {
        double t2 = tamanho2();
        if (t2 <= max * max) return this;
        return mul(max / Math.sqrt(t2));
    }

    /** Mesmo sentido, com o tamanho pedido. */
    public Vetor comTamanho(double t) {
        return normalizado().mul(t);
    }

    /** Perpendicular (gira 90°). */
    public Vetor perpendicular() { return new Vetor(-y, x); }

    public double angulo() { return Math.atan2(y, x); }

    public boolean valido() {
        return Double.isFinite(x) && Double.isFinite(y);
    }

    @Override
    public String toString() {
        return String.format("(%.1f, %.1f)", x, y);
    }
}
