import java.util.*;

public class NfaToDfaConverter {

    public static Automato convert(Automato afn) {
        Automato afd = new Automato(afn.tipo + "_AFD");
        Set<String> alfabeto = afn.getAlfabeto();

        Map<Set<Integer>, Integer> dfaStatesMap = new HashMap<>();
        Queue<Set<Integer>> fila = new LinkedList<>();

        Set<Integer> estadoInicialAfn = new HashSet<>(Collections.singletonList(afn.estadoInicial));
        Set<Integer> estadoInicialAfd = afn.epsilonClosure(estadoInicialAfn);

        int contadorEstadosAfd = 0;
        dfaStatesMap.put(estadoInicialAfd, contadorEstadosAfd);
        fila.add(estadoInicialAfd);

        afd.estadoInicial = contadorEstadosAfd;
        contadorEstadosAfd++;

        while (!fila.isEmpty()) {
            Set<Integer> estadoAtualAfd = fila.poll();
            int idAtualAfd = dfaStatesMap.get(estadoAtualAfd);

            for (int eFinal : afn.estadosFinais) {
                if (estadoAtualAfd.contains(eFinal)) {
                    afd.estadosFinais.add(idAtualAfd);
                    break;
                }
            }

            for (String simbolo : alfabeto) {
                Set<Integer> alcancaveis = afn.move(estadoAtualAfd, simbolo);

                if (!alcancaveis.isEmpty()) {
                    Set<Integer> proximoEstadoAfd = afn.epsilonClosure(alcancaveis);

                    if (!dfaStatesMap.containsKey(proximoEstadoAfd)) {
                        dfaStatesMap.put(proximoEstadoAfd, contadorEstadosAfd);
                        fila.add(proximoEstadoAfd);
                        contadorEstadosAfd++;
                    }

                    afd.addTransicao(idAtualAfd, simbolo, dfaStatesMap.get(proximoEstadoAfd));
                }
            }
        }

        return afd;
    }
}