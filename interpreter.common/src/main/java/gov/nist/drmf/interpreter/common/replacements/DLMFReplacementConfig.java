package gov.nist.drmf.interpreter.common.replacements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;

import java.io.IOException;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class DLMFReplacementConfig {
    private static DLMFReplacementConfig REPL_CONF;

    @JsonProperty("replacementRules")
    public List<DLMFReplacementRule> dlmfRules;

    @JsonProperty("generalReplacements")
    public List<DLMFReplacementRule> rules;

    private DLMFReplacementConfig() {};

    public static DLMFReplacementConfig getInstance() throws IOException {
        if ( REPL_CONF == null || REPL_CONF.dlmfRules == null ) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            REPL_CONF = mapper.readValue(
                    GlobalPaths.PATH_DLMF_REPLACEMENT_RULES.toFile(),
                    DLMFReplacementConfig.class
            );

            DLMFReplacementConfig genRules = mapper.readValue(
                    GlobalPaths.PATH_REPLACEMENT_RULES.toFile(),
                    DLMFReplacementConfig.class
            );

            REPL_CONF.rules = genRules.rules;
        }
        return REPL_CONF;
    }

    public String replace(String input, String link) {
        for ( DLMFReplacementRule rule : rules ) {
            input = rule.replace(input);
        }

        for ( DLMFReplacementRule rule : dlmfRules ) {
            if ( rule.applicable(link) ) {
                input = rule.replace(input);
            }
        }
        return input;
    }
}
