package gov.nist.drmf.interpreter.common.config;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

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

    @JsonSetter("cas")
    private void setCas(String cas) {
        this.cas = cas;
    }

    @JsonGetter("cas")
    public String getCas() {
        return cas;
    }

    @JsonSetter("paths")
    private void setPaths(List<String> paths) {
        this.paths = paths;
    }

    @JsonGetter("paths")
    private List<String> getStringPaths() {
        return this.paths;
    }

    @JsonIgnore
    public List<Path> getPaths() {
        return paths.stream().map(Paths::get).collect(Collectors.toList());
    }

    @JsonIgnore
    public boolean isValid() {
        boolean valid = true;
        for ( Path p : getPaths() )
            valid &= Files.exists(p);
        return valid;
    }
}
