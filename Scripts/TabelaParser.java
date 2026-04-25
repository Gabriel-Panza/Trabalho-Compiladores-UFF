package Scripts;

import java.util.*;

public class TabelaParser {
    Map<String, Map<String, List<String>>> tabela = new HashMap<>();
    List<String> conflitos = new ArrayList<>();

    public TabelaParser(List<RegraGramatical> regras) {
        for (RegraGramatical regra : regras) {
            String nt = regra.naoTerminal;
            tabela.computeIfAbsent(nt, k -> new HashMap<>());

            for (String terminal : regra.first) {
                adicionarEntrada(nt, terminal, regra.producao);
            }

            if (regra.anulavel) {
                for (String terminal : regra.follow) {
                    adicionarEntrada(nt, terminal, regra.producao);
                }
            }
        }
    }

    private void adicionarEntrada(String nt, String terminal, List<String> producao) {
        Map<String, List<String>> linha = tabela.get(nt);
        if (linha.containsKey(terminal)) {
            conflitos.add("Conflito LL(1): [" + nt + ", " + terminal + "] ja possui " +
                          linha.get(terminal) + ", tentando inserir " + producao);
        }
        linha.put(terminal, producao);
    }

    public List<String> consultar(String naoTerminal, String terminal) {
        Map<String, List<String>> linha = tabela.get(naoTerminal);
        if (linha == null) return null;
        return linha.get(terminal);
    }

    public boolean temConflitos() {
        return !conflitos.isEmpty();
    }

    public void imprimir(Set<String> terminais) {
        List<String> termsOrdenados = new ArrayList<>(terminais);
        Collections.sort(termsOrdenados);

        System.out.printf("%-15s", "");
        for (String t : termsOrdenados) {
            System.out.printf("| %-30s", t);
        }
        System.out.println("|");

        String separador = "-".repeat(15);
        for (String ignored : termsOrdenados) {
            separador += "+" + "-".repeat(30);
        }
        separador += "+";
        System.out.println(separador);

        List<String> nts = new ArrayList<>(tabela.keySet());
        Collections.sort(nts);
        for (String nt : nts) {
            System.out.printf("%-15s", nt);
            Map<String, List<String>> linha = tabela.get(nt);
            for (String t : termsOrdenados) {
                List<String> prod = linha.get(t);
                if (prod != null) {
                    String rhs = prod.isEmpty() ? "(vazio)" : String.join(" ", prod);
                    System.out.printf("| %-30s", rhs);
                } else {
                    System.out.printf("| %-30s", "");
                }
            }
            System.out.println("|");
        }
    }
}
