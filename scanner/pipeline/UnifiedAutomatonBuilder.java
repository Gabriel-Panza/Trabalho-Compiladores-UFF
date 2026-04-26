package scanner.pipeline;

import scanner.model.Automato;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UnifiedAutomatonBuilder {
  final List<Automato> automatos = new ArrayList<Automato>();
  private static final int ASCII_SIZE = 256;
  private static final int SINK_COMPONENT_STATE = -1;

  public void addAutomato(Automato automato) {
    automatos.add(automato);
  }

  public record AutomatoStatePointer(String automatoTipo, int estado){}

  public Automato buildFinalAutomato() {
    final Automato resultAutomato = new Automato("FINAL");

    if (automatos.isEmpty()) {
      resultAutomato.estadoInicial = 0;
      return resultAutomato;
    }

    // Prioridade base segue ordem de adicao, mas tokens de identificador ficam por ultimo.
    int priority = 0;
    for (Automato automato : automatos) {
      if (!isIdentifierTipo(automato.tipo)) {
        resultAutomato.setTipoPriority(automato.tipo, priority++);
      }
    }
    for (Automato automato : automatos) {
      if (isIdentifierTipo(automato.tipo)) {
        resultAutomato.setTipoPriority(automato.tipo, priority++);
      }
    }

    // List with initial states of each automaton
    final List<Integer> initialProduct = new ArrayList<>();
    for (Automato automato : automatos) {
      initialProduct.add(automato.estadoInicial);
    }

    // Maps to keep track of the product states and their corresponding unified states
    final Map<List<Integer>, Integer> productToUnified = new HashMap<>();
    // List to retrieve the product state from a unified state index
    final List<List<Integer>> unifiedToProduct = new ArrayList<>();
    final ArrayDeque<Integer> queue = new ArrayDeque<>();

    resultAutomato.estadoInicial = 0;
    productToUnified.put(initialProduct, 0);
    unifiedToProduct.add(initialProduct);
    queue.add(0);

    while (!queue.isEmpty()) {
      int unifiedState = queue.poll(); // Gets first from queue
      // Gets everything that contributes to this state in the unified automaton
      List<Integer> productState = unifiedToProduct.get(unifiedState);

      annotateFinalState(resultAutomato, unifiedState, productState);

      Map<List<Integer>, boolean[]> signatureToChars = new LinkedHashMap<>();
      for (int ascii = 0; ascii < ASCII_SIZE; ascii++) {
        List<Integer> nextProduct = new ArrayList<>(automatos.size());
        for (int i = 0; i < automatos.size(); i++) {
          int next = nextStateForChar(automatos.get(i), productState.get(i), (char) ascii);
          nextProduct.add(next);
        }

        // Nao materializa transicao para produto totalmente sumidouro.
        if (isSinkProduct(nextProduct)) {
          continue;
        }

        boolean[] mask = signatureToChars.computeIfAbsent(nextProduct, k -> new boolean[ASCII_SIZE]);
        mask[ascii] = true;
      }

      for (Map.Entry<List<Integer>, boolean[]> entry : signatureToChars.entrySet()) {
        List<Integer> nextProduct = entry.getKey();
        Integer nextUnifiedState = productToUnified.get(nextProduct);
        if (nextUnifiedState == null) {
          nextUnifiedState = unifiedToProduct.size();
          productToUnified.put(nextProduct, nextUnifiedState);
          unifiedToProduct.add(nextProduct);
          queue.add(nextUnifiedState);
        }

        String regex = maskToRegex(entry.getValue());
        resultAutomato.addTransicao(unifiedState, regex, nextUnifiedState);
      }
    }

    return resultAutomato;
  }

  private void annotateFinalState(Automato unified, int unifiedState, List<Integer> productState) {
    String preferredActiveTipo = null;
    String preferredFinalTipo = null;

    for (int i = 0; i < automatos.size(); i++) {
      Automato source = automatos.get(i);
      int sourceState = productState.get(i);
      if (sourceState == SINK_COMPONENT_STATE) {
        continue;
      }

      preferredActiveTipo = higherPriorityTipo(unified, preferredActiveTipo, source.tipo);
      if (source.estadosFinais.contains(sourceState)) {
        preferredFinalTipo = higherPriorityTipo(unified, preferredFinalTipo, source.tipo);
      }
    }

    if (preferredFinalTipo == null || !preferredFinalTipo.equals(preferredActiveTipo)) {
      return;
    }

    unified.estadosFinais.add(unifiedState);
    unified.finalStateTipos.put(unifiedState, new ArrayList<String>(Collections.singletonList(preferredFinalTipo)));
  }

  private int nextStateForChar(Automato automato, int currentState, char c) {
    if (currentState == SINK_COMPONENT_STATE) {
      return SINK_COMPONENT_STATE;
    }

    Map<String, List<Integer>> transitions = automato.transicoes.get(currentState);
    if (transitions == null) {
      return SINK_COMPONENT_STATE;
    }

    for (Map.Entry<String, List<Integer>> entry : transitions.entrySet()) {
      String regex = entry.getKey();
      if ("ε".equals(regex)) {
        continue;
      }
      if (matchesRegex(c, regex)) {
        List<Integer> destinos = entry.getValue();
        if (destinos != null && !destinos.isEmpty()) {
          return destinos.get(0);
        }
      }
    }

    return SINK_COMPONENT_STATE;
  }

  private boolean matchesRegex(char c, String regex) {
    return String.valueOf(c).matches(regex);
  }

  private boolean isSinkProduct(List<Integer> productState) {
    for (int state : productState) {
      if (state != SINK_COMPONENT_STATE) {
        return false;
      }
    }
    return true;
  }

  private String higherPriorityTipo(Automato unified, String current, String candidate) {
    if (current == null) {
      return candidate;
    }

    int currentPriority = unified.tipoPriority.getOrDefault(current, Integer.MAX_VALUE);
    int candidatePriority = unified.tipoPriority.getOrDefault(candidate, Integer.MAX_VALUE);
    return candidatePriority < currentPriority ? candidate : current;
  }

  private boolean isIdentifierTipo(String tipo) {
    return tipo != null && tipo.toUpperCase().contains("IDENT");
  }

  private String maskToRegex(boolean[] mask) {
    if (isFullMask(mask)) {
      return "[\\s\\S]";
    }

    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < ASCII_SIZE; i++) {
      if (mask[i]) {
        appendEscapedCharClass(sb, (char) i);
      }
    }
    sb.append(']');
    return sb.toString();
  }

  private boolean isFullMask(boolean[] mask) {
    for (boolean accepted : mask) {
      if (!accepted) {
        return false;
      }
    }
    return true;
  }

  private void appendEscapedCharClass(StringBuilder sb, char c) {
    switch (c) {
      case '\\':
        sb.append("\\\\");
        break;
      case '[':
      case ']':
      case '^':
      case '-':
        sb.append('\\').append(c);
        break;
      case '\n':
        sb.append("\\n");
        break;
      case '\r':
        sb.append("\\r");
        break;
      case '\t':
        sb.append("\\t");
        break;
      case '\f':
        sb.append("\\f");
        break;
      default:
        if (Character.isISOControl(c)) {
          sb.append(String.format("\\x%02X", (int) c));
        } else {
          sb.append(c);
        }
    }
  }
}
