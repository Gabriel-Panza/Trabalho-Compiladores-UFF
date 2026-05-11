package parser.model;

public class TokenLido {
    public String tipo;
    public String lexema;
    public int linha;
    public int coluna;
    public String arquivo;

    public TokenLido(String tipo, String lexema, int linha, int coluna) {
        this(tipo, lexema, linha, coluna, null);
    }

    public TokenLido(String tipo, String lexema, int linha, int coluna, String arquivo) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
        this.coluna = coluna;
        this.arquivo = arquivo;
    }

    @Override
    public String toString() {
        String origem = arquivo == null || arquivo.trim().isEmpty() ? "" : arquivo + " ";
        return tipo + "(\"" + lexema + "\") [" + origem + linha + ":" + coluna + "]";
    }
}
