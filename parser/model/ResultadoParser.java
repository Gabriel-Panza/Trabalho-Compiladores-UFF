package Scripts;

import java.util.*;

public class ResultadoParser {
    NoArvore arvore;
    List<ErroSintatico> erros;
    boolean aceito;

    public ResultadoParser(NoArvore arvore, List<ErroSintatico> erros) {
        this.arvore = arvore;
        this.erros = erros;
        this.aceito = erros.isEmpty();
    }
}
