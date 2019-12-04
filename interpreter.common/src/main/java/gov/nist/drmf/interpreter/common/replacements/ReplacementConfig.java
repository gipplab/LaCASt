package gov.nist.drmf.interpreter.common.replacements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class ReplacementConfig {
    private static final Logger LOG = LogManager.getLogger(ReplacementConfig.class.getName());

    private static ReplacementConfig REPL_CONF;

    private List<DLMFConditionalReplacementImpl> dlmfRules;

    private List<ReplacementRule> rules;

    private ReplacementConfig() {};

    public static ReplacementConfig getInstance() {
        if ( REPL_CONF == null || REPL_CONF.dlmfRules == null ) {
            try {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                REPL_CONF = mapper.readValue(
                        GlobalPaths.PATH_DLMF_REPLACEMENT_RULES.toFile(),
                        ReplacementConfig.class
                );

                ReplacementConfig genRules = mapper.readValue(
                        GlobalPaths.PATH_REPLACEMENT_RULES.toFile(),
                        ReplacementConfig.class
                );

                REPL_CONF.rules = genRules.rules;
            } catch ( IOException ioe ) {
                LOG.error("Cannot load replacement rules! " +
                        "Using standard replacements instead", ioe);
            }
        }
        return REPL_CONF;
    }

    @JsonSetter("dlmfReplacementRules")
    public void setDLMFReplacementRules(List<DLMFConditionalReplacementImpl> dlmfRules) {
        this.dlmfRules = dlmfRules;
    }

    @JsonSetter("generalReplacements")
    public void setGeneralReplacementRules(List<ReplacementRule> rules) {
        this.rules = rules;
    }

    /**
     * Replace {@param input} according to the attached rules.
     * @param input the string that will be replaced according to the defined rules
     * @param link a link, if there should be conditional replacements
     * @return the changed input string according to the replacement rules
     */
    public String replace(String input, @Nullable String link) {
        for ( ReplacementRule rule : rules ) {
            input = rule.replace(input);
        }

        if ( link == null ) return input;

        for ( ConditionalReplacementRule rule : dlmfRules ) {
            if ( rule.applicable(link) ) {
                input = rule.replace(input);
            }
        }
        return input;
    }
}
