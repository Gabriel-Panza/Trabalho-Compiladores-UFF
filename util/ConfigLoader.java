package util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigLoader {
    private static final String CONFIG_FILE = "configs.json";
    private static JsonNode rootNode;

    static {
        ObjectMapper mapper = new ObjectMapper();
        try {
            rootNode = mapper.readTree(new File(CONFIG_FILE));
        } catch (IOException e) {
            System.err.println("Aviso: Nao foi possivel carregar o arquivo " + CONFIG_FILE + ". Usando valores padrao.");
        }
    }

    public static String getProperty(String path, String defaultValue) {
        if (rootNode == null) return defaultValue;
        
        String[] parts = path.split("\\.");
        JsonNode current = rootNode;
        for (String part : parts) {
            current = current.path(part);
        }
        
        return current.isMissingNode() || current.isNull() ? defaultValue : current.asText();
    }

    public static String[] getArrayProperty(String path, String defaultValue) {
        if (rootNode == null) return defaultValue.split(",");

        String[] parts = path.split("\\.");
        JsonNode current = rootNode;
        for (String part : parts) {
            current = current.path(part);
        }

        if (current.isArray()) {
            List<String> list = new ArrayList<>();
            for (JsonNode node : current) {
                list.add(node.asText());
            }
            return list.toArray(new String[0]);
        }
        
        return (current.isMissingNode() || current.isNull()) ? defaultValue.split(",") : current.asText().split(",");
    }
}
