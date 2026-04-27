package scanner;

import scanner.model.Automato;
import scanner.persistence.AutomatoJsonRepository;
import scanner.pipeline.RegexToPostfix;
import scanner.pipeline.ThompsonBuilder;
import scanner.pipeline.NfaToDfaConverter;
import scanner.pipeline.DfaMinimizer;
import scanner.pipeline.UnifiedAutomatonBuilder;

import util.ConfigLoader;

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
        String lexicalRulesPath = ConfigLoader.getProperty("Scanner.LexicalRules", "casos_teste/lexico.rules");
        String outputAutomatonPath = ConfigLoader.getProperty("Scanner.OutputAutomaton", "out/automato-final.json");


        List<String[]> regrasRacket = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(lexicalRulesPath));
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("->");
                if (parts.length == 2) {
                    regrasRacket.add(new String[]{parts[0].trim(), parts[1].trim()});
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo de regras lexicas: " + lexicalRulesPath);
            return;
        }

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
            
            Automato afn = ThompsonBuilder.build(regra.postfix(), regra.tipo());
            printStatus("AFN", afn);

            Automato afd = NfaToDfaConverter.convert(afn);
            printStatus("AFD", afd);

            Automato afdMin = DfaMinimizer.minimize(afd);
            printStatus("AFD MINIMIZADO", afdMin);
            
            System.out.println("────────────────────────────────────────────────────────────────");
            builder.addAutomato(afdMin);
        }

        final Automato finalAutomaton = builder.buildFinalAutomato();
        
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
        
        final AutomatoJsonRepository repository = new AutomatoJsonRepository();
        try {
            repository.salvar(finalAutomaton, outputAutomatonPath);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao salvar/carregar o autômato em JSON", e);
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