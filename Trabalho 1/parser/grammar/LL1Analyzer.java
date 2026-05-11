package parser.grammar;

import java.util.*;

public class LL1Analyzer {

    public static List<RegraGramatical> gerarRegras(BNFReader bnf) {
        Set<String> naoTerminais = bnf.regrasBrutas.keySet();

        Map<String, Boolean> nullable = new HashMap<>();
        Map<String, Set<String>> first = new HashMap<>();
        Map<String, Set<String>> follow = new HashMap<>();

        for (String simbolo : bnf.todosSimbolos) {
            nullable.put(simbolo, false);
            first.put(simbolo, new HashSet<>());
            follow.put(simbolo, new HashSet<>());
            if (!naoTerminais.contains(simbolo)) {
                first.get(simbolo).add(simbolo);
            }
        }

        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (String nt : naoTerminais) {
                for (List<String> producao : bnf.regrasBrutas.get(nt)) {
                    if (producao.isEmpty()) {
                        if (!nullable.get(nt)) { nullable.put(nt, true); mudou = true; }
                    } else {
                        boolean todosNullable = true;
                        for (String s : producao) {
                            if (!nullable.get(s)) { todosNullable = false; break; }
                        }
                        if (todosNullable && !nullable.get(nt)) {
                            nullable.put(nt, true); mudou = true;
                        }
                    }
                }
            }
        }

        mudou = true;
        while (mudou) {
            mudou = false;
            for (String nt : naoTerminais) {
                for (List<String> producao : bnf.regrasBrutas.get(nt)) {
                    for (String s : producao) {
                        int tamanhoAntes = first.get(nt).size();
                        first.get(nt).addAll(first.get(s));
                        if (first.get(nt).size() > tamanhoAntes) mudou = true;

                        if (!nullable.get(s)) break;
                    }
                }
            }
        }

        follow.get(bnf.simboloInicial).add("$");
        mudou = true;
        while (mudou) {
            mudou = false;
            for (String nt : naoTerminais) {
                for (List<String> producao : bnf.regrasBrutas.get(nt)) {
                    for (int i = 0; i < producao.size(); i++) {
                        String B = producao.get(i);
                        if (!naoTerminais.contains(B)) continue;

                        int tamanhoAntes = follow.get(B).size();
                        boolean todosNullableAFrente = true;

                        for (int j = i + 1; j < producao.size(); j++) {
                            String beta = producao.get(j);
                            follow.get(B).addAll(first.get(beta));
                            if (!nullable.get(beta)) {
                                todosNullableAFrente = false;
                                break;
                            }
                        }

                        if (todosNullableAFrente) {
                            follow.get(B).addAll(follow.get(nt));
                        }

                        if (follow.get(B).size() > tamanhoAntes) mudou = true;
                    }
                }
            }
        }

        List<RegraGramatical> listaFinal = new ArrayList<>();
        for (String nt : naoTerminais) {
            for (List<String> producao : bnf.regrasBrutas.get(nt)) {

                Set<String> firstDaRegra = new HashSet<>();
                boolean regraAnulavel = true;

                if (producao.isEmpty()) {
                    regraAnulavel = true;
                } else {
                    for (String s : producao) {
                        firstDaRegra.addAll(first.get(s));
                        if (!nullable.get(s)) {
                            regraAnulavel = false;
                            break;
                        }
                    }
                }

                Set<String> followDoNT = new HashSet<>(follow.get(nt));

                listaFinal.add(new RegraGramatical(nt, producao, firstDaRegra, followDoNT, regraAnulavel));
            }
        }

        return listaFinal;
    }
}