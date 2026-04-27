package parser;

import parser.grammar.BNFReader;
import parser.grammar.LL1Analyzer;
import parser.grammar.RegraGramatical;
import parser.model.ErroSintatico;
import parser.model.ResultadoParser;
import parser.model.TokenLido;

import scanner.Scanner;
import scanner.model.Automato;
import scanner.persistence.AutomatoJsonRepository;

import util.ConfigLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ParserMain {

    public static void main(String[] args) throws Exception {
        String arquivoGramatica = ConfigLoader.getProperty("Parser.GrammarBnf", "casos_teste/racket.bnf");
        String automatoJson = ConfigLoader.getProperty("Parser.AutomatonJson", "out/automato-final.json");
        String[] arquivosTeste = ConfigLoader.getArrayProperty("Test.Files", "casos_teste/teste_simples.txt,casos_teste/teste_fibonacci.txt,casos_teste/teste_erro.txt");


        System.out.println("Gerando gramática dinamicamente a partir de: " + arquivoGramatica);
        BNFReader bnf = new BNFReader();
        bnf.ler(arquivoGramatica);

        List<RegraGramatical> regrasGeradas = LL1Analyzer.gerarRegras(bnf);

        Parser parser = new Parser(regrasGeradas, bnf.simboloInicial);
        Automato automato = new AutomatoJsonRepository().carregar(automatoJson);

        for (String arquivoEntrada : arquivosTeste) {
            Path inputPath = Paths.get(arquivoEntrada);
            String nomeBase = inputPath.getFileName().toString().replaceFirst("\\.[^.]+$", "");
            Path scannedPath = Paths.get("out", nomeBase + ".scanned.txt");

            String scanned = Scanner.replaceRecognizedLexemesWithTipo(automato,
                Files.readString(inputPath, StandardCharsets.UTF_8));
            Files.createDirectories(scannedPath.getParent());
            Files.writeString(scannedPath, scanned, StandardCharsets.UTF_8);

            List<TokenLido> tokens = ScannedTokenReader.lerTokens(scannedPath);
            ResultadoParser resultado = parser.analisar(tokens);

            System.out.println("\n================================================================");
            System.out.println("ARQUIVO: " + arquivoEntrada);
            System.out.println("================================================================");

            if (resultado.aceito) {
                System.out.println("RESULTADO: ACEITO");
                System.out.println("\nArvore sintatica:");
                resultado.arvore.imprimir("", true);
            } else {
                System.out.println("RESULTADO: REJEITADO (" + resultado.erros.size() + " erros)");
                System.out.println("\nErros:");
                for (ErroSintatico erro : resultado.erros) {
                    System.out.println("  " + erro);
                }
                System.out.println("\nArvore parcial:");
                resultado.arvore.imprimir("", true);
            }
        }
    }
}
