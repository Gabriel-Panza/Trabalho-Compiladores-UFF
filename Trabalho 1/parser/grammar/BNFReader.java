package parser.grammar;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class BNFReader {
    public String simboloInicial = null;
    public Map<String, List<List<String>>> regrasBrutas = new LinkedHashMap<>();
    public Set<String> todosSimbolos = new HashSet<>();

    public void ler(String pathBNF) throws Exception {
        List<String> linhas = Files.readAllLines(Paths.get(pathBNF));

        int numeroLinha = 0;
        for (String linha : linhas) {
            numeroLinha++;
            linha = linha.trim();
            if (linha.isEmpty() || linha.startsWith("#")) continue;

            String[] partes = linha.split("->");
            if (partes.length != 2) {
                throw new RuntimeException("Erro de sintaxe no BNF em " + pathBNF + ", linha " + numeroLinha + ": " + linha);
            }

            String naoTerminal = partes[0].trim();
            String producaoStr = partes[1].trim();

            if (simboloInicial == null) simboloInicial = naoTerminal;

            regrasBrutas.computeIfAbsent(naoTerminal, k -> new ArrayList<>());
            todosSimbolos.add(naoTerminal);

            List<String> simbolosProducao = new ArrayList<>();
            if (!producaoStr.equals("EPSILON")) {
                for (String s : producaoStr.split("\\s+")) {
                    simbolosProducao.add(s);
                    todosSimbolos.add(s);
                }
            }
            regrasBrutas.get(naoTerminal).add(simbolosProducao);
        }
    }
}
