import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String[][] testCases = {
                { "[0-9][0-9]*.[0-9][0-9]*", "FLOAT" },
                { "ab|c", "STRING" },
                { "[a-z]([a-z|0-9])*", "IDENTIFICADOR" },
                { "while", "PALAVRA_RESERVADA" },
                { "a(b|c)*", "TESTE_A" }
        };

        List<RegexToPostfix.RegraLexica> regrasProntas = new ArrayList<>();
        for (String[] test : testCases) {
            String regex = test[0];
            String tipo = test[1];

            String regexLimpa = regex.replace(" ", "");
            List<String> postfix = RegexToPostfix.convertPostfix(regexLimpa);

            RegexToPostfix.RegraLexica regraLexica = new RegexToPostfix.RegraLexica(tipo, postfix);
            regrasProntas.add(regraLexica);

            System.out.println("Entrada: (" + regex + ", " + tipo + ")");
            System.out.println("Saída:   RegraLexica(\"" + regraLexica.tipo() + "\", " + regraLexica.postfix() + ")\n");
        }

        for (RegexToPostfix.RegraLexica regra : regrasProntas) {
            Automato afn = ThompsonBuilder.build(regra.postfix(), regra.tipo());

            System.out.println("--- AFN (" + afn.tipo + ") ---");
            System.out.println("Inicial: " + afn.estadoInicial);
            System.out.println("Finais: " + afn.estadosFinais);
            System.out.println("Transições: " + afn.transicoes);

            Automato afd = NfaToDfaConverter.convert(afn);

            System.out.println("--- AFD (" + afd.tipo + ") ---");
            System.out.println("Inicial: " + afd.estadoInicial);
            System.out.println("Finais: " + afd.estadosFinais);
            System.out.println("Transições: " + afd.transicoes);
            System.out.println("===================================\n");
        }
    }
}