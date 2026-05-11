package parser.model;

public class ErroSintatico {
    public String mensagem;
    public String arquivo;
    public int linha;
    public int coluna;
    public String tokenEncontrado;
    public String lexema;
    public String esperado;

    public ErroSintatico(String mensagem, int linha, int coluna,
                         String tokenEncontrado, String lexema, String esperado) {
        this(mensagem, null, linha, coluna, tokenEncontrado, lexema, esperado);
    }

    public ErroSintatico(String mensagem, String arquivo, int linha, int coluna,
                         String tokenEncontrado, String lexema, String esperado) {
        this.mensagem = mensagem;
        this.arquivo = arquivo;
        this.linha = linha;
        this.coluna = coluna;
        this.tokenEncontrado = tokenEncontrado;
        this.lexema = lexema;
        this.esperado = esperado;
    }

    @Override
    public String toString() {
        String temArquivo = arquivo == null || arquivo.trim().isEmpty() ? "" : "Arquivo " + arquivo;

        if (linha == 0 && coluna == 0) {
            return temArquivo.isEmpty() ? mensagem : temArquivo + ": " + mensagem;
        }

        String local = String.format("Linha %d, coluna %d: %s", linha, coluna, mensagem);
        return temArquivo.isEmpty() ? local : temArquivo + ", " + local;
    }
}
