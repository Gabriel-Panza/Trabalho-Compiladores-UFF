package scanner;

import scanner.model.Automato;
import scanner.persistence.AutomatoJsonRepository;
import util.ConfigLoader;


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
        String configAutomato = ConfigLoader.getProperty("Scanner.OutputAutomaton", "out/automato-final.json");

        
        if (args.length < 1) {
            System.err.println("Uso: java Scanner <entrada.txt> [saida.txt]");
            System.err.println("Ou configure 'scanner.output_automaton' no compiler.config");
            System.exit(1);
        }

        Path automatoPath = Paths.get(configAutomato);
        Path inputPath = Paths.get(args[0]);
        Path outputPath = args.length >= 2 ? Paths.get(args[1]) : defaultOutputPath(inputPath);


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

    public static String replaceRecognizedLexemesWithTipo(Automato automato, String input) {
        StringBuilder result = new StringBuilder();

        result.append("========================================\n");
        result.append("TOKENS RECONHECIDOS\n");
        result.append("========================================\n");

        int index = 0;
        int contador = 1;

        while (index < input.length()) {
            Match bestMatch = longestMatchAt(automato, input, index);

            if (bestMatch == null) {
                String lexemaErro = String.valueOf(input.charAt(index));

                result.append(String.format(
                    "%03d  %-20s  lexema: \"%s\"%n",
                    contador,
                    LEXICAL_ERROR_MARKER,
                    formatarLexema(lexemaErro)
                ));

                index++;
                contador++;
                continue;
            }

            String lexemaReconhecido = input.substring(index, bestMatch.endExclusive);

            result.append(String.format(
                "%03d  %-20s  lexema: \"%s\"%n",
                contador,
                bestMatch.tipo,
                formatarLexema(lexemaReconhecido)
            ));

            index = bestMatch.endExclusive;
            contador++;
        }

        result.append("========================================\n");

        return result.toString();
    }

    private static String formatarLexema(String lexema) {
        return lexema
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
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