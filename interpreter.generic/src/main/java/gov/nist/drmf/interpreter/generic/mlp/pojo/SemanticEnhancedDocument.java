package gov.nist.drmf.interpreter.generic.mlp.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import gov.nist.drmf.interpreter.common.interfaces.SemanticallyRanked;
import gov.nist.drmf.interpreter.common.pojo.SemanticEnhancedAnnotationStatus;
import gov.nist.drmf.interpreter.pom.moi.MOINode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancedDocument implements SemanticallyRanked {
    @JsonProperty("title")
    private final String title;

    @JsonProperty("formulae")
    private final List<MOIPresentations> formulae;

    public SemanticEnhancedDocument() {
        this.title = "Unknown";
        this.formulae = new LinkedList<>();
    }

    public SemanticEnhancedDocument(SemanticEnhancedDocument copy) {
        this.title = copy.title;
        this.formulae = new LinkedList<>(copy.formulae);
    }

    public SemanticEnhancedDocument(String title, MLPDependencyGraph graph) {
        this.title = title;
        this.formulae = graph.getVertices().stream()
                .sorted(Comparator.comparing(MOINode::getAnnotation))
                .map(MOIPresentations::new)
                .collect(Collectors.toList());
    }

    public String getTitle() {
        return title;
    }

    public List<MOIPresentations> getFormulae() {
        return formulae;
    }

    @JsonIgnore
    public SemanticEnhancedAnnotationStatus getRank() {
        return getRank(this);
    }

    @JsonIgnore
    public static SemanticEnhancedDocument deserialize(String json) throws JsonProcessingException {
        ObjectMapper mapper = getMapper();
        return mapper.readValue(json, SemanticEnhancedDocument.class);
    }

    @JsonIgnore
    public String serialize() throws JsonProcessingException {
        return getMapper().writer(printer).writeValueAsString(this);
    }

    @JsonIgnore
    private static SemanticEnhancedAnnotationStatus getRank(SemanticEnhancedDocument sed) {
        Stream<MOIPresentations> moiStream = sed.getFormulae().stream();

        return moiStream.map( MOIPresentations::getRank )
                .max( Comparator.comparingInt(SemanticEnhancedAnnotationStatus::getRank) )
                .orElse(SemanticEnhancedAnnotationStatus.BASE);
    }

    @JsonIgnore
    private static ObjectMapper mapperInstance;

    @JsonIgnore
    private static DefaultPrettyPrinter printer;

    public static ObjectMapper getMapper() {
        if ( mapperInstance == null ) {
            mapperInstance = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapperInstance.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapperInstance.registerModule(new GuavaModule());

            printer = new DefaultPrettyPrinter();
            printer.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        }
        return mapperInstance;
    }
}
