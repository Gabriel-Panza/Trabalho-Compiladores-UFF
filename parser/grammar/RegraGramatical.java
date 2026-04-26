package Scripts;

import java.util.*;

public class RegraGramatical {
    String naoTerminal;
    List<String> producao;
    Set<String> first;
    Set<String> follow;
    boolean anulavel;

    public RegraGramatical(String naoTerminal, List<String> producao,
                           Set<String> first, Set<String> follow, boolean anulavel) {
        this.naoTerminal = naoTerminal;
        this.producao = producao;
        this.first = first;
        this.follow = follow;
        this.anulavel = anulavel;
    }

    @Override
    public String toString() {
        String rhs = producao.isEmpty() ? "(vazio)" : String.join(" ", producao);
        return naoTerminal + " -> " + rhs;
    }
}
