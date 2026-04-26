package parser;

import parser.grammar.RegraGramatical;
import parser.model.ErroSintatico;
import parser.model.ResultadoParser;
import parser.model.TokenLido;

import java.util.*;

public class ParserMain {
    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("PARSER LL(1) - AUTOMATO DE PILHA");
        System.out.println("================================================================");

        // ── ENTRADA 1: tabela da gramatica (mock) ─────────────────────
        List<RegraGramatical> gramatica = criarGramaticaMock();

        System.out.println("\nGRAMATICA (" + gramatica.size() + " producoes):");
        System.out.println("----------------------------------------------------------------");
        for (RegraGramatical r : gramatica) {
            System.out.println("  " + r);
        }

        // ── Construcao do parser e tabela LL(1) ───────────────────────
        Parser parser = new Parser(gramatica, "Programa");

        System.out.println("\nTABELA LL(1):");
        System.out.println("----------------------------------------------------------------");
        parser.getTabela().imprimir(parser.getTerminais());

        // ── ENTRADA 2: lista de tokens (mock) ─────────────────────────
        System.out.println("\n================================================================");
        System.out.println("TESTE 1: (define x 42)  -  programa valido");
        System.out.println("================================================================");
        executar(parser, criarTokensMock1());

        System.out.println("\n================================================================");
        System.out.println("TESTE 2: (if #t (+ 1 2) 0)  -  programa valido");
        System.out.println("================================================================");
        executar(parser, criarTokensMock2());

        System.out.println("\n================================================================");
        System.out.println("TESTE 3: (define 42)  -  programa com erro");
        System.out.println("================================================================");
        executar(parser, criarTokensMock3());

        System.out.println("\n================================================================");
        System.out.println("TESTE 4: (lambda (x y) (+ x y))  -  programa valido");
        System.out.println("================================================================");
        executar(parser, criarTokensMock4());
    }

    private static void executar(Parser parser, List<TokenLido> tokens) {
        System.out.println("\nTokens de entrada:");
        for (TokenLido t : tokens) {
            System.out.println("  " + t);
        }

        ResultadoParser resultado = parser.analisar(tokens);

        if (resultado.aceito) {
            System.out.println("\nRESULTADO: ACEITO");
            System.out.println("\nArvore sintatica:");
            resultado.arvore.imprimir("", true);
        } else {
            System.out.println("\nRESULTADO: REJEITADO (" + resultado.erros.size() + " erros)");
            System.out.println("\nErros encontrados:");
            for (ErroSintatico erro : resultado.erros) {
                System.out.println("  " + erro);
            }
            System.out.println("\nArvore parcial:");
            resultado.arvore.imprimir("", true);
        }
    }

    // =====================================================================
    // MOCK: Gramatica BNF com First / Follow / Nullable
    // =====================================================================
    // Substituir por dados reais do participante que gera a tabela.
    //
    // Gramatica Racket (subconjunto LL(1)):
    //   Programa   -> ListaExpr
    //   ListaExpr  -> Expr ListaExpr | ε
    //   Expr       -> LPAREN Corpo RPAREN | IDENTIFIER | INTEGER | FLOAT | BOOLEAN
    //   Corpo      -> KW_DEFINE IDENTIFIER Expr
    //                | KW_LAMBDA LPAREN ListaParam RPAREN Expr
    //                | KW_IF Expr Expr Expr
    //                | Expr ListaExpr
    //   ListaParam -> IDENTIFIER ListaParam | ε
    // =====================================================================

    private static List<RegraGramatical> criarGramaticaMock() {
        List<RegraGramatical> regras = new ArrayList<>();

        Set<String> firstExpr = Set.of("LPAREN", "IDENTIFIER", "INTEGER", "FLOAT", "BOOLEAN");

        // Programa -> ListaExpr
        regras.add(new RegraGramatical("Programa",
            List.of("ListaExpr"),
            firstExpr,
            Set.of("$"),
            true));

        // ListaExpr -> Expr ListaExpr
        regras.add(new RegraGramatical("ListaExpr",
            List.of("Expr", "ListaExpr"),
            firstExpr,
            Set.of("$", "RPAREN"),
            false));

        // ListaExpr -> ε
        regras.add(new RegraGramatical("ListaExpr",
            List.of(),
            Set.of(),
            Set.of("$", "RPAREN"),
            true));

        // Expr -> LPAREN Corpo RPAREN
        regras.add(new RegraGramatical("Expr",
            List.of("LPAREN", "Corpo", "RPAREN"),
            Set.of("LPAREN"),
            Set.of("LPAREN", "IDENTIFIER", "INTEGER", "FLOAT", "BOOLEAN", "RPAREN", "$"),
            false));

        // Expr -> IDENTIFIER
        regras.add(new RegraGramatical("Expr",
            List.of("IDENTIFIER"),
            Set.of("IDENTIFIER"),
            Set.of("LPAREN", "IDENTIFIER", "INTEGER", "FLOAT", "BOOLEAN", "RPAREN", "$"),
            false));

        // Expr -> INTEGER
        regras.add(new RegraGramatical("Expr",
            List.of("INTEGER"),
            Set.of("INTEGER"),
            Set.of("LPAREN", "IDENTIFIER", "INTEGER", "FLOAT", "BOOLEAN", "RPAREN", "$"),
            false));

        // Expr -> FLOAT
        regras.add(new RegraGramatical("Expr",
            List.of("FLOAT"),
            Set.of("FLOAT"),
            Set.of("LPAREN", "IDENTIFIER", "INTEGER", "FLOAT", "BOOLEAN", "RPAREN", "$"),
            false));

        // Expr -> BOOLEAN
        regras.add(new RegraGramatical("Expr",
            List.of("BOOLEAN"),
            Set.of("BOOLEAN"),
            Set.of("LPAREN", "IDENTIFIER", "INTEGER", "FLOAT", "BOOLEAN", "RPAREN", "$"),
            false));

        // Corpo -> KW_DEFINE IDENTIFIER Expr
        regras.add(new RegraGramatical("Corpo",
            List.of("KW_DEFINE", "IDENTIFIER", "Expr"),
            Set.of("KW_DEFINE"),
            Set.of("RPAREN"),
            false));

        // Corpo -> KW_LAMBDA LPAREN ListaParam RPAREN Expr
        regras.add(new RegraGramatical("Corpo",
            List.of("KW_LAMBDA", "LPAREN", "ListaParam", "RPAREN", "Expr"),
            Set.of("KW_LAMBDA"),
            Set.of("RPAREN"),
            false));

        // Corpo -> KW_IF Expr Expr Expr
        regras.add(new RegraGramatical("Corpo",
            List.of("KW_IF", "Expr", "Expr", "Expr"),
            Set.of("KW_IF"),
            Set.of("RPAREN"),
            false));

        // Corpo -> Expr ListaExpr   (aplicacao de funcao)
        regras.add(new RegraGramatical("Corpo",
            List.of("Expr", "ListaExpr"),
            firstExpr,
            Set.of("RPAREN"),
            false));

        // ListaParam -> IDENTIFIER ListaParam
        regras.add(new RegraGramatical("ListaParam",
            List.of("IDENTIFIER", "ListaParam"),
            Set.of("IDENTIFIER"),
            Set.of("RPAREN"),
            false));

        // ListaParam -> ε
        regras.add(new RegraGramatical("ListaParam",
            List.of(),
            Set.of(),
            Set.of("RPAREN"),
            true));

        return regras;
    }

    // =====================================================================
    // MOCK: Listas de tokens lidos do programa-fonte
    // =====================================================================
    // Substituir por dados reais do participante que gera a lista de tokens.
    // =====================================================================

    // (define x 42)
    private static List<TokenLido> criarTokensMock1() {
        return List.of(
            new TokenLido("LPAREN",    "(",      1, 1),
            new TokenLido("KW_DEFINE", "define", 1, 2),
            new TokenLido("IDENTIFIER","x",      1, 9),
            new TokenLido("INTEGER",   "42",     1, 11),
            new TokenLido("RPAREN",    ")",      1, 13)
        );
    }

    // (if #t (+ 1 2) 0)
    private static List<TokenLido> criarTokensMock2() {
        return List.of(
            new TokenLido("LPAREN",     "(",   1, 1),
            new TokenLido("KW_IF",      "if",  1, 2),
            new TokenLido("BOOLEAN",    "#t",  1, 5),
            new TokenLido("LPAREN",     "(",   1, 8),
            new TokenLido("IDENTIFIER", "+",   1, 9),
            new TokenLido("INTEGER",    "1",   1, 11),
            new TokenLido("INTEGER",    "2",   1, 13),
            new TokenLido("RPAREN",     ")",   1, 14),
            new TokenLido("INTEGER",    "0",   1, 16),
            new TokenLido("RPAREN",     ")",   1, 17)
        );
    }

    // (define 42)  -  erro: falta IDENTIFIER apos define
    private static List<TokenLido> criarTokensMock3() {
        return List.of(
            new TokenLido("LPAREN",    "(",      2, 1),
            new TokenLido("KW_DEFINE", "define", 2, 2),
            new TokenLido("INTEGER",   "42",     2, 9),
            new TokenLido("RPAREN",    ")",      2, 11)
        );
    }

    // (lambda (x y) (+ x y))
    private static List<TokenLido> criarTokensMock4() {
        return List.of(
            new TokenLido("LPAREN",     "(",      3, 1),
            new TokenLido("KW_LAMBDA",  "lambda", 3, 2),
            new TokenLido("LPAREN",     "(",      3, 9),
            new TokenLido("IDENTIFIER", "x",      3, 10),
            new TokenLido("IDENTIFIER", "y",      3, 12),
            new TokenLido("RPAREN",     ")",      3, 13),
            new TokenLido("LPAREN",     "(",      3, 15),
            new TokenLido("IDENTIFIER", "+",      3, 16),
            new TokenLido("IDENTIFIER", "x",      3, 18),
            new TokenLido("IDENTIFIER", "y",      3, 20),
            new TokenLido("RPAREN",     ")",      3, 21),
            new TokenLido("RPAREN",     ")",      3, 22)
        );
    }
}
