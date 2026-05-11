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
    private static File configFile;
    private static boolean resolveRelativePathsFromConfigDir;

    static {
        ObjectMapper mapper = new ObjectMapper();
        try {
            configFile = resolveConfigFile();
            rootNode = mapper.readTree(configFile);
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
        
        String value = current.isMissingNode() || current.isNull() ? defaultValue : current.asText();
        return resolveConfiguredPath(value);
    }

    public static String[] getArrayProperty(String path, String defaultValue) {
        if (rootNode == null) return splitAndResolve(defaultValue);

        String[] parts = path.split("\\.");
        JsonNode current = rootNode;
        for (String part : parts) {
            current = current.path(part);
        }

        if (current.isArray()) {
            List<String> list = new ArrayList<>();
            for (JsonNode node : current) {
                list.add(resolveConfiguredPath(node.asText()));
            }
            return list.toArray(new String[0]);
        }
        
        return splitAndResolve((current.isMissingNode() || current.isNull()) ? defaultValue : current.asText());
    }

    private static File resolveConfigFile() {
        File localConfig = new File(CONFIG_FILE);
        if (localConfig.isFile()) {
            resolveRelativePathsFromConfigDir = false;
            return localConfig;
        }

        File nestedConfig = new File("Trabalho 1", CONFIG_FILE);
        if (nestedConfig.isFile()) {
            resolveRelativePathsFromConfigDir = true;
            return nestedConfig;
        }

        resolveRelativePathsFromConfigDir = false;
        return localConfig;
    }

    private static String[] splitAndResolve(String value) {
        String[] parts = value.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = resolveConfiguredPath(parts[i].trim());
        }
        return parts;
    }

    private static String resolveConfiguredPath(String value) {
        if (!resolveRelativePathsFromConfigDir || value == null || value.trim().isEmpty()) {
            return value;
        }

        File file = new File(value);
        if (file.isAbsolute()) {
            return value;
        }

        File baseDir = configFile == null ? null : configFile.getParentFile();
        return baseDir == null ? value : new File(baseDir, value).getPath();
    }
}
