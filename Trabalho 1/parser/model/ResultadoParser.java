package parser.model;

import java.util.*;

public class ResultadoParser {
    public NoArvore arvore;
    public List<ErroSintatico> erros;
    public boolean aceito;

    public ResultadoParser(NoArvore arvore, List<ErroSintatico> erros) {
        this.arvore = arvore;
        this.erros = erros;
        this.aceito = erros.isEmpty();
    }
}
