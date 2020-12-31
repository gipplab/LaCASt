package gov.nist.drmf.interpreter.generic.mlp.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.pom.moi.MOINode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancedDocument {
    @JsonProperty("title")
    private final String title;

    @JsonProperty("formulae")
    private final List<MOIPresentations> formulae;

    public SemanticEnhancedDocument() {
        this.title = "Unknown";
        this.formulae = new LinkedList<>();
    }

    public SemanticEnhancedDocument(String title, MLPDependencyGraph graph) {
        this.title = title;
        this.formulae = graph.getVertices().stream()
                .sorted(Comparator.comparing(MOINode::getAnnotation))
                .map(MOIPresentations::new)
                .collect(Collectors.toList());
    }

    public SemanticEnhancedAnnotationStatus getSemanticState() {
        return getRank(this);
    }

    public String getTitle() {
        return title;
    }

    public List<MOIPresentations> getFormulae() {
        return formulae;
    }

    public static SemanticEnhancedDocument deserialize(String json) throws JsonProcessingException {
        ObjectMapper mapper = getMapper();
        return mapper.readValue(json, SemanticEnhancedDocument.class);
    }

    private static SemanticEnhancedAnnotationStatus getRank(SemanticEnhancedDocument sed) {
        Stream<MOIPresentations> moiStream = sed.getFormulae().stream();

        return moiStream.map( MOIPresentations::getStatus )
                .max( Comparator.comparingInt(SemanticEnhancedAnnotationStatus::getRank) )
                .orElse(SemanticEnhancedAnnotationStatus.BASE);
    }

    @JsonIgnore
    private static ObjectMapper mapperInstance;

    public static ObjectMapper getMapper() {
        if ( mapperInstance == null ) {
            mapperInstance = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapperInstance.registerModule(new GuavaModule());
        }
        return mapperInstance;
    }
}
