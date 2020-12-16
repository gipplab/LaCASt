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
    @JsonProperty("install.path")
    private String path;

    private CASConfig(){}

    @JsonSetter("install.path")
    public void setInstallPaths(String path) {
        this.path = path;
    }

    @JsonGetter("install.path")
    public String getStringInstallPath() {
        return this.path;
    }

    @JsonIgnore
    public Path getInstallPath() {
        if ( path == null ) return null;
        return Paths.get(path);
    }

    @JsonIgnore
    public boolean isValid() {
        return path == null || Files.exists(getInstallPath());
    }
}
