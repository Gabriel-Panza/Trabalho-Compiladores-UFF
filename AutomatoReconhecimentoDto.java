import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutomatoReconhecimentoDto {
    public int estadoInicial;
    public Set<Integer> estadosFinais;
    public Map<Integer, Map<String, List<Integer>>> transicoes;
    public Map<Integer, List<String>> finalStateTipos;
    public Map<String, Integer> tipoPriority;

    public static AutomatoReconhecimentoDto fromAutomato(Automato automato) {
        AutomatoReconhecimentoDto dto = new AutomatoReconhecimentoDto();
        dto.estadoInicial = automato.estadoInicial;
        dto.estadosFinais = new HashSet<>(automato.estadosFinais);
        dto.transicoes = deepCopyTransicoes(automato.transicoes);
        dto.finalStateTipos = deepCopyFinalStateTipos(automato.finalStateTipos);
        dto.tipoPriority = new HashMap<>(automato.tipoPriority);
        return dto;
    }

    public Automato toAutomato() {
        Automato automato = new Automato("FINAL");
        automato.estadoInicial = estadoInicial;
        automato.estadosFinais = estadosFinais == null ? new HashSet<>() : new HashSet<>(estadosFinais);
        automato.transicoes = transicoes == null ? new HashMap<>() : deepCopyTransicoes(transicoes);
        automato.finalStateTipos = finalStateTipos == null ? new HashMap<>() : deepCopyFinalStateTipos(finalStateTipos);
        automato.tipoPriority = tipoPriority == null ? new HashMap<>() : new HashMap<>(tipoPriority);
        return automato;
    }

    private static Map<Integer, Map<String, List<Integer>>> deepCopyTransicoes(
            Map<Integer, Map<String, List<Integer>>> source) {
        Map<Integer, Map<String, List<Integer>>> copy = new HashMap<>();
        for (Map.Entry<Integer, Map<String, List<Integer>>> stateEntry : source.entrySet()) {
            Map<String, List<Integer>> transitions = new HashMap<>();
            for (Map.Entry<String, List<Integer>> transitionEntry : stateEntry.getValue().entrySet()) {
                transitions.put(transitionEntry.getKey(), new ArrayList<>(transitionEntry.getValue()));
            }
            copy.put(stateEntry.getKey(), transitions);
        }
        return copy;
    }

    private static Map<Integer, List<String>> deepCopyFinalStateTipos(Map<Integer, List<String>> source) {
        Map<Integer, List<String>> copy = new HashMap<>();
        for (Map.Entry<Integer, List<String>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }
}

