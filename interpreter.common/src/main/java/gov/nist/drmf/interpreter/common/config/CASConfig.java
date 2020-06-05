package gov.nist.drmf.interpreter.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class CASConfig {
    @JsonProperty("cas")
    private String cas;

    @JsonProperty("paths")
    private List<String> paths = new LinkedList<>();

    private CASConfig(){}

    public String getCas() {
        return cas;
    }

    public List<Path> getPaths() {
        return paths.stream().map(Paths::get).collect(Collectors.toList());
    }

    public boolean isValid() {
        boolean valid = true;
        for ( Path p : getPaths() )
            valid &= Files.exists(p);
        return valid;
    }
}
