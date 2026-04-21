import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class UnifiedAutomatonBuilder {
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

    // Priority is determined by the order of addition
    for (int i = 0; i < automatos.size(); i++) {
      resultAutomato.setTipoPriority(automatos.get(i).tipo, i);
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

      Map<String, boolean[]> signatureToChars = new LinkedHashMap<>();
      for (int ascii = 0; ascii < ASCII_SIZE; ascii++) {
        List<Integer> nextProduct = new ArrayList<>(automatos.size());
        for (int i = 0; i < automatos.size(); i++) {
          int next = nextStateForChar(automatos.get(i), productState.get(i), (char) ascii);
          nextProduct.add(next);
        }

        String signature = buildSignature(nextProduct);
        boolean[] mask = signatureToChars.computeIfAbsent(signature, k -> new boolean[ASCII_SIZE]);
        mask[ascii] = true;
      }

      for (Map.Entry<String, boolean[]> entry : signatureToChars.entrySet()) {
        List<Integer> nextProduct = parseSignature(entry.getKey());
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
    for (int i = 0; i < automatos.size(); i++) {
      Automato source = automatos.get(i);
      int sourceState = productState.get(i); // TODO: fix this, productState should be a Map<Automato, List<Integer>>, since we can have multiple states from the same automaton in the product
      if (sourceState != SINK_COMPONENT_STATE && source.estadosFinais.contains(sourceState)) {
        unified.estadosFinais.add(unifiedState);
        unified.addFinalStateTipo(unifiedState, source.tipo);
      }
    }
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

  private String buildSignature(List<Integer> productState) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < productState.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(productState.get(i));
    }
    return sb.toString();
  }

  private List<Integer> parseSignature(String signature) {
    String[] parts = signature.split(",");
    List<Integer> product = new ArrayList<>(parts.length);
    for (String part : parts) {
      product.add(Integer.parseInt(part));
    }
    return product;
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
