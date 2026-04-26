package parser.model;

import java.util.*;

public class NoArvore {
    public String simbolo;
    public String lexema;
    public List<NoArvore> filhos;
    public int linha;
    public int coluna;

    public NoArvore(String simbolo) {
        this.simbolo = simbolo;
        this.filhos = new ArrayList<>();
    }

    public void adicionarFilho(NoArvore filho) {
        filhos.add(filho);
    }

    public void imprimir(String prefixo, boolean ultimo) {
        System.out.println(prefixo + (ultimo ? "`-- " : "|-- ") + formatarNo());
        String novoPrefixo = prefixo + (ultimo ? "    " : "|   ");
        for (int i = 0; i < filhos.size(); i++) {
            filhos.get(i).imprimir(novoPrefixo, i == filhos.size() - 1);
        }
    }

    private String formatarNo() {
        if (lexema != null) {
            return simbolo + " \"" + lexema + "\"";
        }
        if (filhos.isEmpty()) {
            return simbolo + " (vazio)";
        }
        return simbolo;
    }
}
