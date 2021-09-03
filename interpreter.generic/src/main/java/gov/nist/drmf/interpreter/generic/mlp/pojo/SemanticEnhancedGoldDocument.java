package gov.nist.drmf.interpreter.generic.mlp.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancedGoldDocument extends SemanticEnhancedDocument {

    @JsonProperty("id")
    private int id;

    @JsonProperty("pid")
    private int pid;

    @JsonProperty("eid")
    private String eid;

    public SemanticEnhancedGoldDocument() {}

    @JsonIgnore
    public static SemanticEnhancedGoldDocument deserialize(String json) throws JsonProcessingException {
        ObjectMapper mapper = getMapper();
        return mapper.readValue(json, SemanticEnhancedGoldDocument.class);
    }

    @JsonIgnore
    public static List<SemanticEnhancedGoldDocument> deserializeGold(Path path) throws IOException {
        ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
        if ( Files.isDirectory(path) ) {
            return Files.list(path).map(Path::toFile).map(f -> {
                try {
                    return mapper.readValue(f, SemanticEnhancedGoldDocument.class);
                } catch (IOException e) {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            String docsStr = Files.readString(path);
            if ( docsStr.startsWith("[") ) {
                SemanticEnhancedGoldDocument[] docs = mapper.readValue(docsStr, SemanticEnhancedGoldDocument[].class);
                return Arrays.asList(docs.clone());
            } else {
                return List.of(SemanticEnhancedGoldDocument.deserialize(docsStr));
            }
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getEid() {
        return eid;
    }

    public void setEid(String eid) {
        this.eid = eid;
    }
}
