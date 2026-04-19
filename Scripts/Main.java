package Scripts;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String[][] regrasRacket = {
            { "\\(", "LPAREN" },
            { "\\)", "RPAREN" },
            { "define", "KW_DEFINE" },
            { "lambda", "KW_LAMBDA" },
            { "if", "KW_IF" },
            { "#t|#f", "BOOLEAN" },
            { "[a-zA-Z!$%&*/:<=>?^_~][a-zA-Z0-9!$%&*/:<=>?^_~+-.@]*", "IDENTIFIER" },
            { "[0-9]+", "INTEGER" },
            { "[0-9]+.[0-9]*", "FLOAT" }
        };

        System.out.println("================================================================");
        System.out.println("GERADOR DE SCANNERS - RACKET EDITION");
        System.out.println("================================================================");

        List<RegexToPostfix.RegraLexica> regrasProntas = new ArrayList<>();
        System.out.println("\nFASE DE TOKENIZAÇÃO E POSTFIX:");
        System.out.println("----------------------------------------------------------------");
        for (String[] regra : regrasRacket) {
            String regex = regra[0];
            String tipo = regra[1];

            List<String> postfix = RegexToPostfix.convertPostfix(regex);
            RegexToPostfix.RegraLexica regraLexica = new RegexToPostfix.RegraLexica(tipo, postfix);
            regrasProntas.add(regraLexica);

            System.out.printf("  %-15s -> %s\n", "[" + tipo + "]", regex);
            System.out.printf("  %-15s    Postfix: %s\n\n", "", postfix);
        }
        System.out.println("----------------------------------------------------------------");
        System.out.println("CONSTRUÇÃO E OTIMIZAÇÃO DE AUTÔMATOS:");
        System.out.println("----------------------------------------------------------------");

        for (RegexToPostfix.RegraLexica regra : regrasProntas) {
            System.out.println("\n💎 TOKEN: " + regra.tipo());
            
            // AFND (Thompson)
            Automato afn = ThompsonBuilder.build(regra.postfix(), regra.tipo());
            printStatus("AFN", afn);

            // AFD (Subset Construction)
            Automato afd = NfaToDfaConverter.convert(afn);
            printStatus("AFD", afd);

            // AFD Miniminizado (Hopcroft)
            Automato afdMin = DfaMinimizer.minimize(afd);
            printStatus("AFD MINIMIZADO", afdMin);
            
            System.out.println("────────────────────────────────────────────────────────────────");
        }
    }

    private static void printStatus(String fase, Automato auto) {
        System.out.printf("   %-16s | Início: %-2d | Finais: %-12s | Transições: %d\n", 
            fase, 
            auto.estadoInicial, 
            auto.estadosFinais,
            countTransitions(auto));
    }

    private static int countTransitions(Automato auto) {
        int count = 0;
        for (var entry : auto.transicoes.values()) {
            for (var list : entry.values()) {
                count += list.size();
            }
        }
        return count;
    }
}