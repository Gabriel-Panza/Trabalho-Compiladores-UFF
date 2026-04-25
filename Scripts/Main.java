package Scripts;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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


        final UnifiedAutomatonBuilder builder = new UnifiedAutomatonBuilder();
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
            builder.addAutomato(afdMin);
        }

        final Automato finalAutomaton = builder.buildFinalAutomato();

        final String arquivoSaidaJson = "out/automato-final.json";
        final AutomatoJsonRepository repository = new AutomatoJsonRepository();
        try {
            repository.salvar(finalAutomaton, arquivoSaidaJson);
            System.out.println("Autômato salvo em JSON: " + arquivoSaidaJson);

            Automato automatoCarregado = repository.carregar(arquivoSaidaJson);
            System.out.println("Autômato carregado do JSON. Finais: " + automatoCarregado.estadosFinais);
            System.out.println("Tipos finais carregados: " + automatoCarregado.finalStateTipos);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao salvar/carregar o autômato em JSON", e);
        }

        System.out.println("--- AUTÔMATO FINAL UNIFICADO ---");
        System.out.println("Inicial: " + finalAutomaton.estadoInicial);
        System.out.println("Finais: " + new LinkedHashSet<>(finalAutomaton.estadosFinais));
        System.out.println("Tipos de Estados Finais: " + finalAutomaton.finalStateTipos);
        for (int estado : finalAutomaton.getTodosEstados()) {
            for (var transition : finalAutomaton.transicoes.getOrDefault(estado, Map.of()).entrySet()) {
                String simbolo = transition.getKey();
                List<Integer> destinos = transition.getValue();
                for (int destino : destinos) {
                    System.out.println("Transição: " + estado + " --" + simbolo + "--> " + destino);
                }
            }
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