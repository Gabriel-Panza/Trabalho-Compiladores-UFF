package Scripts;

public class ErroSintatico {
    String mensagem;
    int linha;
    int coluna;
    String tokenEncontrado;
    String esperado;

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
