package futebol.selecoes;

import java.awt.*;
import java.util.List;

/**
 * Seleção nacional: nome, sigla FIFA, artigo (para "gol DO Japão", "gol DA França")
 * e a bandeira desenhada na ficha.
 *
 * {@link #ADVERSARIOS} contém as 47 seleções classificadas para a Copa do Mundo
 * de 2026, além do Brasil (lista final, fechada após as repescagens de 31/03/2026).
 */
public record Selecao(String nome, String sigla, String artigo, String confederacao, Bandeira bandeira) {

    /** "do Japão", "da França", "dos Estados Unidos", "de Portugal". */
    public String comPreposicao() {
        return switch (artigo) {
            case "o" -> "do " + nome;
            case "a" -> "da " + nome;
            case "os" -> "dos " + nome;
            case "as" -> "das " + nome;
            default -> "de " + nome;
        };
    }

    // ------------------------------------------------------------------
    // Cores
    // ------------------------------------------------------------------
    private static final Color BRANCO = Color.WHITE;
    private static final Color PRETO = new Color(20, 20, 20);
    private static final Color VERMELHO = new Color(206, 17, 38);
    private static final Color VERDE = new Color(0, 122, 61);
    private static final Color AZUL = new Color(0, 56, 147);
    private static final Color AMARELO = new Color(252, 209, 22);
    private static final Color AZUL_CLARO = new Color(116, 172, 223);

    private static Color c(int r, int g, int b) { return new Color(r, g, b); }
    private static double[] p(double... v) { return v; }
    private static Bandeira b() { return Bandeira.nova(); }

    // ------------------------------------------------------------------
    // Brasil
    // ------------------------------------------------------------------
    public static final Selecao BRASIL = new Selecao("Brasil", "BRA", "o", "CONMEBOL",
            b().fundo(c(0, 156, 59))
                    .poligono(c(255, 223, 0), -0.88, 0, 0, -0.62, 0.88, 0, 0, 0.62)
                    .circulo(0, 0, 0.38, c(0, 39, 118))
                    .poligono(BRANCO, -0.36, -0.06, 0.36, 0.08, 0.36, 0.14, -0.36, 0.0));

    // ------------------------------------------------------------------
    // Os 47 adversários (Copa do Mundo 2026)
    // ------------------------------------------------------------------
    public static final List<Selecao> ADVERSARIOS = List.of(
            // ---------- Anfitriões ----------
            new Selecao("Canadá", "CAN", "o", "CONCACAF",
                    b().verticais(p(1, 2, 1), VERMELHO, BRANCO, VERMELHO)
                            .poligono(VERMELHO, 0, -0.55, 0.1, -0.3, 0.3, -0.38, 0.22, -0.05, 0.42, -0.12, 0.38, 0.05,
                                    0.06, 0.3, 0.05, 0.55, -0.05, 0.55, -0.06, 0.3, -0.38, 0.05, -0.42, -0.12,
                                    -0.22, -0.05, -0.3, -0.38, -0.1, -0.3)),
            new Selecao("México", "MEX", "o", "CONCACAF",
                    b().verticais(c(0, 104, 71), BRANCO, VERMELHO)
                            .circulo(0, 0, 0.24, c(140, 90, 40))
                            .circulo(0, 0.08, 0.14, c(70, 130, 60))),
            new Selecao("Estados Unidos", "USA", "os", "CONCACAF",
                    listrasEUA()),

            // ---------- AFC ----------
            new Selecao("Austrália", "AUS", "a", "AFC",
                    b().fundo(c(1, 33, 105)).uniao(-1, -1, 1.05, 0.95)
                            .estrela(-0.48, 0.5, 0.26, 7, BRANCO, false)
                            .estrela(0.5, -0.55, 0.13, 7, BRANCO, false)
                            .estrela(0.78, -0.05, 0.13, 7, BRANCO, false)
                            .estrela(0.25, 0.05, 0.13, 7, BRANCO, false)
                            .estrela(0.5, 0.62, 0.15, 7, BRANCO, false)),
            new Selecao("Irã", "IRN", "o", "AFC",
                    b().horizontais(c(35, 159, 64), BRANCO, c(218, 0, 0))
                            .circulo(0, 0, 0.16, c(218, 0, 0)).circulo(0, 0, 0.09, BRANCO)),
            new Selecao("Japão", "JPN", "o", "AFC",
                    b().fundo(BRANCO).circulo(0, 0, 0.55, c(188, 0, 45))),
            new Selecao("Jordânia", "JOR", "a", "AFC",
                    b().horizontais(PRETO, BRANCO, c(0, 122, 61))
                            .poligono(c(206, 17, 38), -1, -1, 0.2, 0, -1, 1)
                            .estrela(-0.55, 0, 0.17, 7, BRANCO, false)),
            new Selecao("Coreia do Sul", "KOR", "a", "AFC",
                    coreia()),
            new Selecao("Catar", "QAT", "o", "AFC",
                    qatar()),
            new Selecao("Arábia Saudita", "KSA", "a", "AFC",
                    b().fundo(c(0, 108, 53))
                            .ret(-0.55, -0.35, 1.1, 0.08, BRANCO).ret(-0.45, -0.2, 0.9, 0.06, BRANCO)
                            .ret(-0.5, -0.07, 0.25, 0.06, BRANCO).ret(-0.1, -0.07, 0.6, 0.06, BRANCO)
                            .ret(-0.6, 0.3, 1.1, 0.07, BRANCO).poligono(BRANCO, 0.5, 0.26, 0.65, 0.335, 0.5, 0.41)),
            new Selecao("Uzbequistão", "UZB", "o", "AFC",
                    b().horizontais(p(1, 0.08, 1, 0.08, 1), c(0, 153, 181), VERMELHO, BRANCO, VERMELHO, c(30, 181, 58))
                            .crescente(-0.55, -0.62, 0.22, 0.1, BRANCO, c(0, 153, 181))
                            .estrela(-0.15, -0.72, 0.07, BRANCO).estrela(0.1, -0.72, 0.07, BRANCO)
                            .estrela(-0.15, -0.5, 0.07, BRANCO).estrela(0.1, -0.5, 0.07, BRANCO)),
            new Selecao("Iraque", "IRQ", "o", "AFC",
                    b().horizontais(c(206, 17, 38), BRANCO, PRETO)
                            .ret(-0.45, -0.1, 0.18, 0.2, c(0, 122, 61)).ret(-0.15, -0.12, 0.08, 0.22, c(0, 122, 61))
                            .ret(0.05, -0.08, 0.4, 0.06, c(0, 122, 61)).ret(0.3, -0.16, 0.06, 0.2, c(0, 122, 61))),

            // ---------- CAF ----------
            new Selecao("Argélia", "ALG", "a", "CAF",
                    b().verticais(c(0, 98, 51), BRANCO)
                            .crescente(0, 0, 0.45, 0.13, c(210, 16, 52), BRANCO)
                            .estrela(0.2, 0, 0.18, c(210, 16, 52))),
            new Selecao("Cabo Verde", "CPV", "", "CAF",
                    caboVerde()),
            new Selecao("RD Congo", "COD", "a", "CAF",
                    b().fundo(c(0, 127, 255))
                            .poligono(c(247, 214, 24), -1, 0.75, 0.75, -1, 1, -1, 1, -0.75, -0.75, 1, -1, 1)
                            .poligono(c(206, 16, 33), -1, 0.95, 0.95, -1, 1, -1, 1, -0.95, -0.95, 1, -1, 1)
                            .estrela(-0.55, -0.55, 0.3, c(247, 214, 24))),
            new Selecao("Costa do Marfim", "CIV", "a", "CAF",
                    b().verticais(c(247, 127, 0), BRANCO, c(0, 158, 96))),
            new Selecao("Egito", "EGY", "o", "CAF",
                    b().horizontais(c(206, 17, 38), BRANCO, PRETO)
                            .poligono(c(192, 147, 0), 0, -0.25, 0.2, -0.1, 0.12, 0.25, -0.12, 0.25, -0.2, -0.1)),
            new Selecao("Gana", "GHA", "", "CAF",
                    b().horizontais(c(206, 17, 38), c(252, 209, 22), c(0, 107, 63))
                            .estrela(0, 0.03, 0.3, PRETO)),
            new Selecao("Marrocos", "MAR", "o", "CAF",
                    b().fundo(c(193, 39, 45)).estrela(0, 0.04, 0.45, 5, c(0, 98, 51), true)),
            new Selecao("Senegal", "SEN", "o", "CAF",
                    b().verticais(c(0, 133, 63), c(253, 239, 66), c(227, 27, 35))
                            .estrela(0, 0.02, 0.25, c(0, 133, 63))),
            new Selecao("África do Sul", "RSA", "a", "CAF",
                    africaDoSul()),
            new Selecao("Tunísia", "TUN", "a", "CAF",
                    b().fundo(c(231, 0, 19)).circulo(0, 0, 0.48, BRANCO)
                            .crescente(-0.05, 0, 0.34, 0.11, c(231, 0, 19), BRANCO)
                            .estrela(0.1, 0, 0.17, c(231, 0, 19))),

            // ---------- CONCACAF ----------
            new Selecao("Curaçao", "CUW", "", "CONCACAF",
                    b().fundo(c(0, 43, 127)).ret(-1, 0.25, 2, 0.22, c(249, 232, 20))
                            .estrela(-0.55, -0.5, 0.2, BRANCO).estrela(-0.2, -0.2, 0.14, BRANCO)),
            new Selecao("Haiti", "HAI", "o", "CONCACAF",
                    b().horizontais(c(0, 32, 159), c(210, 16, 52))
                            .ret(-0.38, -0.3, 0.76, 0.6, BRANCO)
                            .poligono(c(1, 106, 22), -0.25, 0.22, 0, -0.2, 0.25, 0.22)
                            .ret(-0.03, -0.22, 0.06, 0.32, c(1, 106, 22))),
            new Selecao("Panamá", "PAN", "o", "CONCACAF",
                    b().fundo(BRANCO).ret(0, -1, 1, 1, c(218, 18, 26)).ret(-1, 0, 1, 1, c(0, 82, 147))
                            .estrela(-0.45, -0.45, 0.25, c(0, 82, 147)).estrela(0.45, 0.48, 0.25, c(218, 18, 26))),

            // ---------- CONMEBOL ----------
            new Selecao("Argentina", "ARG", "a", "CONMEBOL",
                    b().horizontais(AZUL_CLARO, BRANCO, AZUL_CLARO).sol(0, 0, 0.2, c(246, 180, 14))),
            new Selecao("Colômbia", "COL", "a", "CONMEBOL",
                    b().horizontais(p(2, 1, 1), c(252, 209, 22), c(0, 56, 147), c(206, 17, 38))),
            new Selecao("Equador", "ECU", "o", "CONMEBOL",
                    b().horizontais(p(2, 1, 1), c(255, 221, 0), c(3, 78, 162), c(237, 28, 36))
                            .circulo(0, 0, 0.22, c(120, 160, 200)).circulo(0, -0.05, 0.12, c(80, 140, 60))
                            .poligono(c(120, 80, 40), -0.15, -0.28, 0, -0.2, 0.15, -0.28, 0, -0.34)),
            new Selecao("Paraguai", "PAR", "o", "CONMEBOL",
                    b().horizontais(c(213, 43, 30), BRANCO, c(0, 56, 168))
                            .anel(0, 0, 0.2, 0.05, c(0, 120, 60)).estrela(0, 0, 0.1, c(240, 200, 0))),
            new Selecao("Uruguai", "URU", "o", "CONMEBOL",
                    uruguai()),

            // ---------- OFC ----------
            new Selecao("Nova Zelândia", "NZL", "a", "OFC",
                    b().fundo(c(1, 33, 105)).uniao(-1, -1, 1.05, 0.95)
                            .estrela(0.45, -0.55, 0.17, BRANCO).estrela(0.45, -0.55, 0.11, VERMELHO)
                            .estrela(0.75, -0.05, 0.17, BRANCO).estrela(0.75, -0.05, 0.11, VERMELHO)
                            .estrela(0.2, 0.0, 0.15, BRANCO).estrela(0.2, 0.0, 0.09, VERMELHO)
                            .estrela(0.45, 0.62, 0.19, BRANCO).estrela(0.45, 0.62, 0.12, VERMELHO)),

            // ---------- UEFA ----------
            new Selecao("Áustria", "AUT", "a", "UEFA",
                    b().horizontais(c(237, 41, 57), BRANCO, c(237, 41, 57))),
            new Selecao("Bélgica", "BEL", "a", "UEFA",
                    b().verticais(PRETO, c(253, 218, 36), c(239, 51, 64))),
            new Selecao("Bósnia e Herzegovina", "BIH", "a", "UEFA",
                    bosnia()),
            new Selecao("Croácia", "CRO", "a", "UEFA",
                    croacia()),
            new Selecao("Tchéquia", "CZE", "a", "UEFA",
                    b().horizontais(BRANCO, c(215, 20, 26)).poligono(c(17, 69, 126), -1, -1, 0.1, 0, -1, 1)),
            new Selecao("Inglaterra", "ENG", "a", "UEFA",
                    b().fundo(BRANCO).cruz(1, 0.36, c(206, 17, 36))),
            new Selecao("França", "FRA", "a", "UEFA",
                    b().verticais(c(0, 35, 149), BRANCO, c(237, 41, 57))),
            new Selecao("Alemanha", "GER", "a", "UEFA",
                    b().horizontais(PRETO, c(221, 0, 0), c(255, 206, 0))),
            new Selecao("Holanda", "NED", "a", "UEFA",
                    b().horizontais(c(174, 28, 40), BRANCO, c(33, 70, 139))),
            new Selecao("Noruega", "NOR", "a", "UEFA",
                    b().fundo(c(186, 12, 47)).cruzNordica(BRANCO, 0.5).cruzNordica(c(0, 32, 91), 0.26)),
            new Selecao("Portugal", "POR", "", "UEFA",
                    b().verticais(p(2, 3), c(0, 102, 0), c(255, 0, 0))
                            .circulo(-0.2, 0, 0.36, c(255, 220, 0)).circulo(-0.2, 0, 0.24, c(255, 0, 0))
                            .ret(-0.33, -0.15, 0.26, 0.32, BRANCO).circulo(-0.2, 0.0, 0.07, c(0, 56, 147))),
            new Selecao("Escócia", "SCO", "a", "UEFA",
                    b().fundo(c(0, 94, 184)).aspa(BRANCO, 0.4)),
            new Selecao("Espanha", "ESP", "a", "UEFA",
                    b().horizontais(p(1, 2, 1), c(170, 21, 27), c(241, 191, 0), c(170, 21, 27))
                            .ret(-0.55, -0.22, 0.32, 0.4, c(170, 21, 27)).ret(-0.49, -0.16, 0.2, 0.28, c(241, 191, 0))
                            .ret(-0.6, -0.3, 0.06, 0.55, c(200, 200, 200)).ret(-0.17, -0.3, 0.06, 0.55, c(200, 200, 200))),
            new Selecao("Suécia", "SWE", "a", "UEFA",
                    b().fundo(c(0, 106, 167)).cruzNordica(c(254, 204, 0), 0.34)),
            new Selecao("Suíça", "SUI", "a", "UEFA",
                    b().fundo(c(218, 41, 28)).cruz(0.6, 0.36, BRANCO)),
            new Selecao("Turquia", "TUR", "a", "UEFA",
                    b().fundo(c(227, 10, 23)).crescente(-0.15, 0, 0.5, 0.14, BRANCO, c(227, 10, 23))
                            .estrela(0.4, 0, 0.22, BRANCO))
    );

    // ------------------------------------------------------------------
    // Bandeiras com mais detalhes
    // ------------------------------------------------------------------

    private static Bandeira listrasEUA() {
        Color verm = c(178, 34, 52), azul = c(60, 59, 110);
        Color[] faixas = new Color[9];
        for (int i = 0; i < 9; i++) faixas[i] = i % 2 == 0 ? verm : BRANCO;
        Bandeira b = b().horizontais(faixas).ret(-1, -1, 1.05, 1.0, azul);
        for (int lin = 0; lin < 4; lin++)
            for (int col = 0; col < 4; col++)
                b.estrela(-0.85 + col * 0.25 + (lin % 2) * 0.12, -0.85 + lin * 0.22, 0.07, BRANCO);
        return b;
    }

    private static Bandeira coreia() {
        Bandeira b = b().fundo(BRANCO)
                .poligono(c(205, 46, 58), -0.42, 0, -0.41, -0.1, -0.35, -0.25, -0.2, -0.38, 0, -0.42, 0.2, -0.38,
                        0.35, -0.25, 0.41, -0.1, 0.42, 0)
                .poligono(c(0, 71, 160), -0.42, 0, -0.41, 0.1, -0.35, 0.25, -0.2, 0.38, 0, 0.42, 0.2, 0.38,
                        0.35, 0.25, 0.41, 0.1, 0.42, 0)
                .circulo(-0.21, 0, 0.21, c(205, 46, 58)).circulo(0.21, 0, 0.21, c(0, 71, 160));
        Color preto = c(0, 0, 0);
        // Trigramas (simplificados) nos quatro cantos
        double[][] cantos = {{-0.75, -0.62}, {0.6, -0.62}, {-0.75, 0.48}, {0.6, 0.48}};
        for (double[] k : cantos)
            for (int i = 0; i < 3; i++) b.ret(k[0], k[1] + i * 0.07, 0.17, 0.045, preto);
        return b;
    }

    private static Bandeira qatar() {
        Color bordo = c(138, 21, 56);
        Bandeira b = b().fundo(bordo).ret(-1, -1, 0.5, 2, BRANCO);
        double dentes = 9, h = 2 / dentes;
        for (int i = 0; i < dentes; i++) {
            double y = -1 + i * h;
            b.poligono(BRANCO, -0.51, y, -0.25, y + h / 2, -0.51, y + h);
        }
        return b;
    }

    private static Bandeira caboVerde() {
        Bandeira b = b().fundo(c(0, 56, 147))
                .ret(-1, 0.12, 2, 0.12, BRANCO).ret(-1, 0.24, 2, 0.12, c(207, 32, 39)).ret(-1, 0.36, 2, 0.12, BRANCO);
        for (int i = 0; i < 10; i++) {
            double a = i * 2 * Math.PI / 10;
            b.estrela(-0.2 + Math.cos(a) * 0.45, 0.3 + Math.sin(a) * 0.45, 0.08, c(247, 214, 24));
        }
        return b;
    }

    private static Bandeira africaDoSul() {
        Color verm = c(224, 60, 49), azul = c(0, 20, 137), verde = c(0, 119, 73),
                amarelo = c(255, 184, 28);
        return b().horizontais(verm, azul)
                .poligono(BRANCO, -1, -1, -0.55, -1, 0.05, -0.3, 1, -0.3, 1, 0.3, 0.05, 0.3, -0.55, 1, -1, 1)
                .poligono(verde, -1, -0.8, -0.72, -0.8, -0.05, -0.18, 1, -0.18, 1, 0.18, -0.05, 0.18, -0.72, 0.8, -1, 0.8)
                .poligono(amarelo, -1, -0.62, -0.32, 0, -1, 0.62)
                .poligono(PRETO, -1, -0.48, -0.48, 0, -1, 0.48);
    }

    private static Bandeira uruguai() {
        Color azul = c(0, 56, 168);
        Color[] faixas = new Color[9];
        for (int i = 0; i < 9; i++) faixas[i] = i % 2 == 0 ? BRANCO : azul;
        return b().horizontais(faixas).ret(-1, -1, 1.0, 1.1, BRANCO).sol(-0.45, -0.42, 0.22, c(252, 209, 22));
    }

    private static Bandeira bosnia() {
        Color azul = c(0, 35, 149), amarelo = c(254, 203, 0);
        Bandeira b = b().fundo(azul).poligono(amarelo, -0.3, -1, 0.7, -1, 0.7, 1);
        for (int i = 0; i < 6; i++) {
            double t = i / 5.0;
            b.estrela(-0.55 + t * 0.95, -0.85 + t * 1.75, 0.1, BRANCO);
        }
        return b;
    }

    private static Bandeira croacia() {
        Color verm = c(255, 0, 0);
        Bandeira b = b().horizontais(verm, BRANCO, c(23, 23, 150));
        double lado = 0.13, x0 = -0.325, y0 = -0.35;
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 5; j++)
                b.ret(x0 + j * lado, y0 + i * lado, lado + 0.005, lado + 0.005, (i + j) % 2 == 0 ? verm : BRANCO);
        return b;
    }
}
