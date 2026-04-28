package parser;

import parser.grammar.RegraGramatical;
import parser.grammar.TabelaParser;
import parser.model.ErroSintatico;
import parser.model.NoArvore;
import parser.model.ResultadoParser;
import parser.model.TokenLido;

import java.util.*;

public class Parser {
    private static final int MAX_ERROS = 10;

    private TabelaParser tabela;
    private String simboloInicial;
    private Set<String> terminais;
    private Set<String> naoTerminais;
    private Map<String, Set<String>> conjuntosFollow;

    public Parser(List<RegraGramatical> regras, String simboloInicial) {
        this.simboloInicial = simboloInicial;
        this.naoTerminais = new HashSet<>();
        this.terminais = new HashSet<>();
        this.conjuntosFollow = new HashMap<>();

        for (RegraGramatical regra : regras) {
            naoTerminais.add(regra.naoTerminal);
            conjuntosFollow
                .computeIfAbsent(regra.naoTerminal, k -> new HashSet<>())
                .addAll(regra.follow);
        }

        for (RegraGramatical regra : regras) {
            for (String simbolo : regra.producao) {
                if (!naoTerminais.contains(simbolo)) {
                    terminais.add(simbolo);
                }
            }
        }
        terminais.add("$");

        this.tabela = new TabelaParser(regras);

        if (tabela.temConflitos()) {
            System.out.println("AVISO: Conflitos LL(1) detectados:");
            for (String c : tabela.conflitos) {
                System.out.println("  " + c);
            }
        }
    }

    // Traduz nomes internos de terminais/não-terminais para texto legível pelo usuário
    private String descricao(String simbolo) {
        switch (simbolo) {
            case "LPAREN":    return "(";
            case "RPAREN":    return ")";
            case "KW_DEFINE": return "define";
            case "KW_LAMBDA": return "lambda";
            case "KW_IF":     return "if";
            case "IDENTIFIER":return "um identificador";
            case "INTEGER":   return "um número inteiro";
            case "FLOAT":     return "um número decimal";
            case "BOOLEAN":   return "um booleano (#t ou #f)";
            case "STRING":    return "uma string";
            case "QUOTE":     return "'";
            case "$":         return "fim de arquivo";
            case "Expr":      return "uma expressão";
            case "ListaExpr": return "uma lista de expressões";
            case "Corpo":     return "o corpo de uma expressão";
            case "ListaParam":return "uma lista de parâmetros";
            case "Programa":  return "um programa";
            default:          return "'" + simbolo + "'";
        }
    }

    public ResultadoParser analisar(List<TokenLido> tokens) {
        List<ErroSintatico> erros = new ArrayList<>();
        int posicao = 0;
        int errosTentados = 0;

        Stack<String> pilha = new Stack<>();
        Stack<NoArvore> pilhaNos = new Stack<>();

        pilha.push("$");
        pilha.push(simboloInicial);

        NoArvore raiz = new NoArvore(simboloInicial);
        pilhaNos.push(null);
        pilhaNos.push(raiz);

        int limiteIteracoes = (tokens.size() + 1) * naoTerminais.size() * 50;
        int iteracoes = 0;

        while (!pilha.isEmpty() && iteracoes < limiteIteracoes) {
            iteracoes++;
            String topo = pilha.peek();
            boolean eofAtingido = posicao >= tokens.size();
            String tipoAtual = eofAtingido ? "$" : tokens.get(posicao).tipo;
            TokenLido tokenAtual = eofAtingido ? null : tokens.get(posicao);

            if (topo.equals("$")) {
                if (!tipoAtual.equals("$")) {
                    errosTentados++;
                    if (errosTentados <= MAX_ERROS) {
                        erros.add(new ErroSintatico(
                            "O código continua após o fim esperado do programa. Verifique se há parênteses ou expressões sobrando.",
                            tokenAtual.linha, tokenAtual.coluna, tipoAtual, tokenAtual.lexema, "$"));
                    }
                }
                pilha.pop();
                pilhaNos.pop();
                break;
            }

            if (terminais.contains(topo)) {
                if (topo.equals(tipoAtual)) {
                    pilha.pop();
                    NoArvore no = pilhaNos.pop();
                    if (no != null && tokenAtual != null) {
                        no.lexema = tokenAtual.lexema;
                        no.linha = tokenAtual.linha;
                        no.coluna = tokenAtual.coluna;
                    }
                    posicao++;
                } else if (eofAtingido) {
                    // Terminal esperado mas arquivo terminou — gera UMA mensagem clara e para
                    errosTentados++;
                    if (errosTentados <= MAX_ERROS) {
                        int ultimaLinha = tokens.isEmpty() ? 1 : tokens.get(tokens.size() - 1).linha;
                        int ultimaColuna = tokens.isEmpty() ? 1
                            : tokens.get(tokens.size() - 1).coluna
                              + tokens.get(tokens.size() - 1).lexema.length();
                        long rparens = pilha.stream().filter("RPAREN"::equals).count();
                        String msg;
                        if (rparens == 1) {
                            msg = "O arquivo terminou antes do esperado. Há 1 parêntese não fechado.";
                        } else if (rparens > 1) {
                            msg = "O arquivo terminou antes do esperado. Há " + rparens + " parênteses não fechados.";
                        } else {
                            msg = "O arquivo terminou antes do esperado. Esperava-se " + descricao(topo) + ".";
                        }
                        erros.add(new ErroSintatico(msg, ultimaLinha, ultimaColuna, "$", "<fim de arquivo>", topo));
                    }
                    break;
                } else {
                    errosTentados++;
                    if (errosTentados <= MAX_ERROS) {
                        String lexAtual = tokenAtual.lexema;
                        erros.add(new ErroSintatico(
                            "Esperava-se " + descricao(topo) + " mas foi encontrado '" + lexAtual + "'.",
                            tokenAtual.linha, tokenAtual.coluna,
                            tipoAtual, lexAtual, topo));
                    }
                    pilha.pop();
                    pilhaNos.pop();
                }
            } else if (naoTerminais.contains(topo)) {
                List<String> producao = tabela.consultar(topo, tipoAtual);

                if (producao != null) {
                    pilha.pop();
                    NoArvore noPai = pilhaNos.pop();

                    List<NoArvore> filhos = new ArrayList<>();
                    for (String simbolo : producao) {
                        NoArvore filho = new NoArvore(simbolo);
                        noPai.adicionarFilho(filho);
                        filhos.add(filho);
                    }

                    for (int i = producao.size() - 1; i >= 0; i--) {
                        pilha.push(producao.get(i));
                        pilhaNos.push(filhos.get(i));
                    }
                } else {
                    Set<String> follow = conjuntosFollow.getOrDefault(topo, Collections.emptySet());
                    String lexAtual = tokenAtual != null ? tokenAtual.lexema : "<fim de arquivo>";
                    int linhaErro = tokenAtual != null ? tokenAtual.linha : 0;
                    int colunaErro = tokenAtual != null ? tokenAtual.coluna : 0;

                    if (follow.contains(tipoAtual) || tipoAtual.equals("$")) {
                        errosTentados++;
                        if (errosTentados <= MAX_ERROS) {
                            erros.add(new ErroSintatico(
                                "Falta " + descricao(topo) + " antes de '" + lexAtual + "'.",
                                linhaErro, colunaErro, tipoAtual, lexAtual, topo));
                        }
                        pilha.pop();
                        pilhaNos.pop();
                    } else {
                        errosTentados++;
                        if (errosTentados <= MAX_ERROS) {
                            erros.add(new ErroSintatico(
                                "O símbolo '" + lexAtual + "' não é válido nesta posição.",
                                linhaErro, colunaErro, tipoAtual, lexAtual, topo));
                        }
                        posicao++;
                    }
                }
            } else {
                pilha.pop();
                pilhaNos.pop();
            }
        }

        if (iteracoes >= limiteIteracoes) {
            erros.add(new ErroSintatico(
                "A análise sintática foi interrompida: número máximo de passos atingido. O código pode conter erros graves.",
                0, 0, "", "", ""));
        }

        if (errosTentados > MAX_ERROS) {
            erros.add(new ErroSintatico(
                "... e mais " + (errosTentados - MAX_ERROS) + " erro(s) omitido(s). Corrija os acima e tente novamente.",
                0, 0, "", "", ""));
        }

        return new ResultadoParser(raiz, erros);
    }

    public TabelaParser getTabela() {
        return tabela;
    }

    public Set<String> getTerminais() {
        return terminais;
    }
}
