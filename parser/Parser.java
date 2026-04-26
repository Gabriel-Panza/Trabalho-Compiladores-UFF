package parser;

import parser.grammar.RegraGramatical;
import parser.grammar.TabelaParser;
import parser.model.ErroSintatico;
import parser.model.NoArvore;
import parser.model.ResultadoParser;
import parser.model.TokenLido;

import java.util.*;

public class Parser {
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

    public ResultadoParser analisar(List<TokenLido> tokens) {
        List<ErroSintatico> erros = new ArrayList<>();
        int posicao = 0;

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
            String tipoAtual = posicao < tokens.size() ? tokens.get(posicao).tipo : "$";
            TokenLido tokenAtual = posicao < tokens.size() ? tokens.get(posicao) : null;

            if (topo.equals("$")) {
                if (!tipoAtual.equals("$")) {
                    erros.add(new ErroSintatico(
                        "Tokens extras apos o fim esperado do programa",
                        tokenAtual.linha, tokenAtual.coluna, tipoAtual, "$"));
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
                } else {
                    erros.add(new ErroSintatico(
                        "Token inesperado",
                        tokenAtual != null ? tokenAtual.linha : 0,
                        tokenAtual != null ? tokenAtual.coluna : 0,
                        tipoAtual, topo));
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
                    if (follow.contains(tipoAtual) || tipoAtual.equals("$")) {
                        erros.add(new ErroSintatico(
                            "Producao ausente (sincronizando)",
                            tokenAtual != null ? tokenAtual.linha : 0,
                            tokenAtual != null ? tokenAtual.coluna : 0,
                            tipoAtual, topo));
                        pilha.pop();
                        pilhaNos.pop();
                    } else {
                        erros.add(new ErroSintatico(
                            "Token inesperado (descartando entrada)",
                            tokenAtual != null ? tokenAtual.linha : 0,
                            tokenAtual != null ? tokenAtual.coluna : 0,
                            tipoAtual, topo));
                        posicao++;
                    }
                }
            } else {
                pilha.pop();
                pilhaNos.pop();
            }
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
