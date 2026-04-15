import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class UnifiedAutomatonBuilder {
  final List<Automato> automatos = new ArrayList<Automato>();


  public void addAutomato(Automato automato) {
    automatos.add(automato);
  }

  public record AutomatoStatePointer(String automatoTipo, int estado){}

  public Automato buildFinalAutomato() {
    //Inicialização
    final Automato resultAutomato = new Automato("FINAL");
    resultAutomato.estadoInicial = 0;
    int automatosNoEstadoFinal = 0;
    final Map<String, Automato> tipoToAutomato = new HashMap<>();
    final List<AutomatoStatePointer> estadosAtuais = new ArrayList<>();
    for (Automato automato : automatos) {
      tipoToAutomato.put(automato.tipo, automato);
      estadosAtuais.add(new AutomatoStatePointer(automato.tipo, automato.estadoInicial));
    }

    // Construção do automato unificado
    while (automatosNoEstadoFinal < estadosAtuais.size()) {
      // Verifica os automatos em estados finais, e move os pointers daqueles que não estão
      for (AutomatoStatePointer pointer : estadosAtuais) {
        Automato automato = tipoToAutomato.get(pointer.automatoTipo);
        if (automato.estadosFinais.contains(pointer.estado)) {
          // TODO: Evitar o problema de um estado final ser contado mais de uma vez
          automatosNoEstadoFinal++;
        }

        for (Map.Entry<Integer, Map<String, List<Integer>>> entry : automato.transicoes.entrySet()) {
          int origem = entry.getKey();
          Map<String, List<Integer>> transicoes = entry.getValue();
          for (Map.Entry<String, List<Integer>> trans : transicoes.entrySet()) {
            String simbolo = trans.getKey();
            List<Integer> destinos = trans.getValue();
            final List<Character> acceptedChars = getAcceptedCharactersByRegex(simbolo);

          }
        }

      }
    }
    return resultAutomato;
  }

  private List<Character> getAcceptedCharactersByRegex(String regex) {
    // Implementação para extrair os caracteres aceitos por um regex
    final List<Character> acceptedChars = new ArrayList<>(); // Considerando caracteres ASCII
    int index = 0;
    for (char c = 0; c < 256; c++) {
      if (String.valueOf(c).matches(regex)) {
        acceptedChars.add(c);
      }
    }
    return acceptedChars;
  }

  private List<List<Character>> getDisjointCharacterSets(List<List<Character>> characterSets) {
    // Implementação para obter conjuntos disjuntos de caracteres
    // Isso pode ser feito usando um algoritmo de união de intervalos ou similar
    final List<List<Character>> disjointSets = new ArrayList<>();
    disjointSets.add(getIntersection(characterSets));
    for (int i = 1; i < characterSets.size(); i++) {

    }

    return disjointSets;
  }

  private List<Character> getIntersection(List<List<Character>> sets) {
    // Implementação para obter a interseção de conjuntos de caracteres
    final List<Character> intersection = new ArrayList<>();
    if (sets.isEmpty()) return intersection;
    for (Character c : sets.getFirst()) {
      boolean inAllSets = true;
      for (List<Character> set : sets) {
        if (!set.contains(c)) {
          inAllSets = false;
          break;
        }
      }
      if (inAllSets) {
        intersection.add(c);
      }
    }
    return intersection;
  }

  private List<Character> getUnion(List<List<Character>> sets) {
    // Implementação para obter a união de conjuntos de caracteres
    final List<Character> union = new ArrayList<>();
    for (List<Character> set : sets) {
      for (Character c : set) {
        if (!union.contains(c)) {
          union.add(c);
        }
      }
    }
    return union;
  }

  private List<Character> getDifference(List<Character> setA, List<Character> setB) {
    // Implementação para obter a diferença entre dois conjuntos de caracteres
    final List<Character> difference = new ArrayList<>();
    for (Character c : setA) {
      if (!setB.contains(c)) {
        difference.add(c);
      }
    }
    return difference;
  }

}
