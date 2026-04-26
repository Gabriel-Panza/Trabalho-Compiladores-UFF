package parser.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GramaticaRacket {

    public static List<RegraGramatical> criarGramatica() {
        List<RegraGramatical> regras = new ArrayList<>();
        Set<String> firstExpr = Set.of("LPAREN", "IDENTIFIER", "INTEGER", "FLOAT", "BOOLEAN");

        // Programa -> ListaExpr
        regras.add(new RegraGramatical("Programa",
            List.of("ListaExpr"), firstExpr, Set.of("$"), true));

        // ListaExpr -> Expr ListaExpr | ε
        regras.add(new RegraGramatical("ListaExpr",
            List.of("Expr", "ListaExpr"), firstExpr, Set.of("$", "RPAREN"), false));
        regras.add(new RegraGramatical("ListaExpr",
            List.of(), Set.of(), Set.of("$", "RPAREN"), true));

        // Expr -> LPAREN Corpo RPAREN | IDENTIFIER | INTEGER | FLOAT | BOOLEAN
        Set<String> followExpr = Set.of("LPAREN", "IDENTIFIER", "INTEGER", "FLOAT", "BOOLEAN", "RPAREN", "$");
        regras.add(new RegraGramatical("Expr",
            List.of("LPAREN", "Corpo", "RPAREN"), Set.of("LPAREN"), followExpr, false));
        regras.add(new RegraGramatical("Expr",
            List.of("IDENTIFIER"), Set.of("IDENTIFIER"), followExpr, false));
        regras.add(new RegraGramatical("Expr",
            List.of("INTEGER"), Set.of("INTEGER"), followExpr, false));
        regras.add(new RegraGramatical("Expr",
            List.of("FLOAT"), Set.of("FLOAT"), followExpr, false));
        regras.add(new RegraGramatical("Expr",
            List.of("BOOLEAN"), Set.of("BOOLEAN"), followExpr, false));

        // Corpo -> KW_DEFINE IDENTIFIER Expr
        //        | KW_LAMBDA LPAREN ListaParam RPAREN Expr
        //        | KW_IF Expr Expr Expr
        //        | Expr ListaExpr
        regras.add(new RegraGramatical("Corpo",
            List.of("KW_DEFINE", "IDENTIFIER", "Expr"),
            Set.of("KW_DEFINE"), Set.of("RPAREN"), false));
        regras.add(new RegraGramatical("Corpo",
            List.of("KW_LAMBDA", "LPAREN", "ListaParam", "RPAREN", "Expr"),
            Set.of("KW_LAMBDA"), Set.of("RPAREN"), false));
        regras.add(new RegraGramatical("Corpo",
            List.of("KW_IF", "Expr", "Expr", "Expr"),
            Set.of("KW_IF"), Set.of("RPAREN"), false));
        regras.add(new RegraGramatical("Corpo",
            List.of("Expr", "ListaExpr"), firstExpr, Set.of("RPAREN"), false));

        // ListaParam -> IDENTIFIER ListaParam | ε
        regras.add(new RegraGramatical("ListaParam",
            List.of("IDENTIFIER", "ListaParam"),
            Set.of("IDENTIFIER"), Set.of("RPAREN"), false));
        regras.add(new RegraGramatical("ListaParam",
            List.of(), Set.of(), Set.of("RPAREN"), true));

        return regras;
    }
}
