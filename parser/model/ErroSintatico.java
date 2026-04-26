package parser.model;

public class ErroSintatico {
    public String mensagem;
    public int linha;
    public int coluna;
    public String tokenEncontrado;
    public String esperado;

    public ErroSintatico(String mensagem, int linha, int coluna,
                         String tokenEncontrado, String esperado) {
        this.mensagem = mensagem;
        this.linha = linha;
        this.coluna = coluna;
        this.tokenEncontrado = tokenEncontrado;
        this.esperado = esperado;
    }

    @Override
    public String toString() {
        return String.format("[%d:%d] %s (encontrado: %s, esperado: %s)",
            linha, coluna, mensagem, tokenEncontrado, esperado);
    }
}
