package Scripts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AutomatoJsonRepository {
    private final ObjectMapper objectMapper;

    public AutomatoJsonRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void salvar(Automato automato, String caminhoRelativo) throws IOException {
        Path path = resolveRelativePath(caminhoRelativo);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        AutomatoReconhecimentoDto dto = AutomatoReconhecimentoDto.fromAutomato(automato);
        objectMapper.writeValue(path.toFile(), dto);
    }

    public Automato carregar(String caminhoRelativo) throws IOException {
        Path path = resolveRelativePath(caminhoRelativo);
        AutomatoReconhecimentoDto dto = objectMapper.readValue(path.toFile(), AutomatoReconhecimentoDto.class);
        return dto.toAutomato();
    }

    private Path resolveRelativePath(String caminhoRelativo) {
        return Paths.get(caminhoRelativo).toAbsolutePath().normalize();
    }
}
