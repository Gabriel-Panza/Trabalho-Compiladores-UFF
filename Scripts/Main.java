package Scripts;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String[][] regrasRacket = {
            { "[ \\t\\n\\r]+", "WHITESPACE" },
            { ";[^\\n]*", "COMMENT" },
            { "'", "QUOTE" },
            { "\\(", "LPAREN" },
            { "\\)", "RPAREN" },
            { "define", "KW_DEFINE" },
            { "lambda", "KW_LAMBDA" },
            { "if", "KW_IF" },
            { "#t|#f", "BOOLEAN" },
            { "[a-zA-Z!$%&*/:<=>?^_~][a-zA-Z0-9!$%&*/:<=>?^_~+-.@]*", "IDENTIFIER" },
            { "[0-9]+", "INTEGER" },
            { "[0-9]+.[0-9]*", "FLOAT" },
            { "\"[^\"]*\"", "STRING" }
        };

        System.out.println("================================================================");
        System.out.println("GERADOR DE SCANNERS - PARSER P/ RACKET");
        System.out.println("================================================================");

        List<RegexToPostfix.RegraLexica> regrasProntas = new ArrayList<>();
        System.out.println("----------------------------------------------------------------");
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
            System.out.println("\nTOKEN: " + regra.tipo());
            
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
            Files.writeString(Paths.get("Scripts/Scanner.java"), scannerString.replace("$AUTOMATO_FILE$", "\"" + arquivoSaidaJson + "\""), StandardCharsets.UTF_8);
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

    private static final String scannerString = """
package Scripts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

    public class Scanner {
        private static final int INVALID_STATE = -1;
        private static final String LEXICAL_ERROR_MARKER = "<ERRO_LEXICO>";

        public static void main(String[] args) {
            if (args.length < 1 || args.length > 2) {
                System.err.println("Uso: java Scanner <automato.json> <entrada.txt> [saida.txt]");
                System.exit(1);
            }

            Path automatoPath = Paths.get($AUTOMATO_FILE$);
            Path inputPath = Paths.get(args[0]);
            Path outputPath = args.length == 2 ? Paths.get(args[1]) : defaultOutputPath(inputPath);

            AutomatoJsonRepository repository = new AutomatoJsonRepository();

            try {
                Automato automato = repository.carregar(automatoPath.toString());
                String input = Files.readString(inputPath, StandardCharsets.UTF_8);
                String output = replaceRecognizedLexemesWithTipo(automato, input);

                Path parent = outputPath.toAbsolutePath().normalize().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(outputPath, output, StandardCharsets.UTF_8);

                System.out.println("Arquivo processado com sucesso.");
                System.out.println("Entrada: " + inputPath.toAbsolutePath().normalize());
                System.out.println("Saida: " + outputPath.toAbsolutePath().normalize());
            } catch (IOException e) {
                throw new RuntimeException("Erro de I/O ao processar scanner", e);
            }
        }

        static String replaceRecognizedLexemesWithTipo(Automato automato, String input) {
            StringBuilder result = new StringBuilder();
            int index = 0;

            while (index < input.length()) {
                Match bestMatch = longestMatchAt(automato, input, index);
                if (bestMatch == null) {
                    //result.append(input.charAt(index);
                    result.append(LEXICAL_ERROR_MARKER);
                    index++;
                    continue;
                }

                result.append(bestMatch.tipo);
                index = bestMatch.endExclusive;
            }

            return result.toString();
        }

        private static Match longestMatchAt(Automato automato, String input, int start) {
            int state = automato.estadoInicial;
            int lastAcceptEnd = -1;
            String lastAcceptTipo = null;

            for (int i = start; i < input.length(); i++) {
                int next = nextState(automato, state, input.charAt(i));
                if (next == INVALID_STATE) {
                    break;
                }

                state = next;
                if (automato.estadosFinais.contains(state)) {
                    lastAcceptEnd = i + 1;
                    lastAcceptTipo = preferredTipoForState(automato, state);
                }
            }

            if (lastAcceptEnd == -1 || lastAcceptTipo == null) {
                return null;
            }

            return new Match(lastAcceptEnd, lastAcceptTipo);
        }

        private static int nextState(Automato automato, int currentState, char c) {
            Map<String, List<Integer>> transitions = automato.transicoes.get(currentState);
            if (transitions == null) {
                return INVALID_STATE;
            }

            int selected = INVALID_STATE;
            for (Map.Entry<String, List<Integer>> entry : transitions.entrySet()) {
                String regex = entry.getKey();
                if ("ε".equals(regex)) {
                    continue;
                }
                if (!String.valueOf(c).matches(regex)) {
                    continue;
                }

                List<Integer> destinos = entry.getValue();
                if (destinos == null || destinos.isEmpty()) {
                    continue;
                }

                int candidate = destinos.get(0);
                if (selected != INVALID_STATE && selected != candidate) {
                    throw new IllegalStateException(
                        "Transicoes ambiguas no estado " + currentState + " para caractere '" + c + "'");
                }
                selected = candidate;
            }

            return selected;
        }

        private static String preferredTipoForState(Automato automato, int state) {
            String preferred = automato.getPreferredTipoForState(state);
            if (preferred != null) {
                return preferred;
            }

            List<String> tipos = automato.finalStateTipos.get(state);
            if (tipos == null || tipos.isEmpty()) {
                return null;
            }
            return tipos.get(0);
        }

        private static Path defaultOutputPath(Path inputPath) {
            String fileName = inputPath.getFileName() == null ? "saida" : inputPath.getFileName().toString();
            int extIndex = fileName.lastIndexOf('.');
            String baseName = extIndex <= 0 ? fileName : fileName.substring(0, extIndex);
            return Paths.get("out", baseName + ".scanned.txt");
        }

        private static class Match {
            private final int endExclusive;
            private final String tipo;

            private Match(int endExclusive, String tipo) {
                this.endExclusive = endExclusive;
                this.tipo = tipo;
            }
        }
    }
    """;
}