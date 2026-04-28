package parser.model;

public class ErroSintatico {
    public String mensagem;
    public int linha;
    public int coluna;
    public String tokenEncontrado;
    public String lexema;
    public String esperado;

    public ErroSintatico(String mensagem, int linha, int coluna,
                         String tokenEncontrado, String lexema, String esperado) {
        this.mensagem = mensagem;
        this.linha = linha;
        this.coluna = coluna;
        this.tokenEncontrado = tokenEncontrado;
        this.lexema = lexema;
        this.esperado = esperado;
    }

    @Override
    public String toString() {
        if (linha == 0 && coluna == 0) {
            return mensagem;
        }
        return String.format("Linha %d, coluna %d: %s", linha, coluna, mensagem);
    }
}
