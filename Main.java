import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String[][] testCases = {
                //{ "[0-9][0-9]*.[0-9][0-9]*", "FLOAT" },
                //{ "ab|c", "STRING" },
            {"if", "IF"},
            {"ifi", "IFI"},
            { "[a-z]([a-z0-9])*", "IDENTIFICADOR" },
            { "while", "PALAVRA_RESERVADA" },
                //{ "a(b|c)*", "TESTE_A" }
        };

        List<RegexToPostfix.RegraLexica> regrasProntas = new ArrayList<>();
        for (String[] test : testCases) {
            String regex = test[0];
            String tipo = test[1];

            List<String> postfix = RegexToPostfix.convertPostfix(regex);

            RegexToPostfix.RegraLexica regraLexica = new RegexToPostfix.RegraLexica(tipo, postfix);
            regrasProntas.add(regraLexica);

            System.out.println("Entrada: (" + regex + ", " + tipo + ")");
            System.out.println("Saída:   RegraLexica(\"" + regraLexica.tipo() + "\", " + regraLexica.postfix() + ")\n");
        }

        final UnifiedAutomatonBuilder builder = new UnifiedAutomatonBuilder();
        for (RegexToPostfix.RegraLexica regra : regrasProntas) {
            Automato afn = ThompsonBuilder.build(regra.postfix(), regra.tipo());

            System.out.println("--- AFN (" + afn.tipo + ") ---");
            System.out.println("Inicial: " + afn.estadoInicial);
            System.out.println("Finais: " + afn.estadosFinais);
            System.out.println("Transições: " + afn.transicoes);
            System.out.println("===================================\n");

            Automato afd = NfaToDfaConverter.convert(afn);

            System.out.println("--- AFD (" + afd.tipo + ") ---");
            System.out.println("Inicial: " + afd.estadoInicial);
            System.out.println("Finais: " + afd.estadosFinais);
            System.out.println("Transições: " + afd.transicoes);
            System.out.println("===================================\n");

            Automato afdMin = DfaMinimizer.minimize(afd);

            System.out.println("--- AFD MINIMIZADO (" + afdMin.tipo + ") ---");
            System.out.println("Inicial: " + afdMin.estadoInicial);
            System.out.println("Finais: " + afdMin.estadosFinais);
            System.out.println("Transições: " + afdMin.transicoes);
            System.out.println("===================================\n");
            System.out.println();
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
}