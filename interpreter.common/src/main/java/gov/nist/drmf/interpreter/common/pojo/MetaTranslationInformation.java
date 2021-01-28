package gov.nist.drmf.interpreter.common.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.latex.RelationalComponents;
import gov.nist.drmf.interpreter.common.latex.Relations;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "subEquations", "freeVariables", "constraints", "tokenTranslations"
})
public class MetaTranslationInformation implements Serializable {
    @JsonProperty("freeVariables")
    private List<String> freeVariables;

    @JsonProperty("constraints")
    private List<String> constraints;

    @JsonProperty("subEquations")
    private List<String> subEquations;

    @JsonProperty("tokenTranslations")
    private Map<String, String> tokenTranslationInformation;

    public MetaTranslationInformation() {
        tokenTranslationInformation = new HashMap<>();
        freeVariables = new LinkedList<>();
        constraints = new LinkedList<>();
        subEquations = new LinkedList<>();
    }

    public MetaTranslationInformation(TranslationInformation ti) {
        this();
        if ( ti == null ) return;

        if ( ti.getFreeVariables() != null && ti.getFreeVariables().getFreeVariables() != null ) {
            freeVariables.addAll(
                    ti.getFreeVariables().getFreeVariables()
            );
        }

        if ( ti.getPartialTranslations().isEmpty() ) {
            buildRelationalComponents(ti);
        } else {
            for ( TranslationInformation subTi : ti.getPartialTranslations() ) {
                buildRelationalComponents(subTi);
            }
        }

        constraints.addAll(ti.getTranslatedConstraints());

        InformationLogger logger = ti.getTranslationInformation();
        if ( logger != null ) {
            tokenTranslationInformation.putAll( logger.getGeneralTranslationInformation() );
            tokenTranslationInformation.putAll( logger.getMacroTranslationInformation() );
        }
    }

    private void buildRelationalComponents(TranslationInformation ti) {
        RelationalComponents relComps = ti.getRelationalComponents();
        if ( relComps != null ) {
            LinkedList<String> comps = new LinkedList<>(relComps.getComponents());
            LinkedList<Relations> rels = new LinkedList<>(relComps.getRelations());
            if ( rels.isEmpty() && !comps.isEmpty() ) {
                subEquations.add(comps.getFirst());
            } else {
                while ( !rels.isEmpty() && comps.size() >= 2 ) {
                    Relations rel = rels.removeFirst();
                    if ( rel == null ) continue;
                    subEquations.add(
                            comps.removeFirst() + " " + rel.getSymbol() + " " + comps.get(0)
                    );
                }
            }
        }
    }

    public List<String> getFreeVariables() {
        return freeVariables;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public List<String> getSubEquations() {
        return subEquations;
    }

    public Map<String, String> getTokenTranslationInformation() {
        return tokenTranslationInformation;
    }

    public void setFreeVariables(List<String> freeVariables) {
        this.freeVariables = freeVariables;
    }

    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }

    public void setSubEquations(List<String> subEquations) {
        this.subEquations = subEquations;
    }

    public void setTokenTranslationInformation(Map<String, String> tokenTranslationInformation) {
        this.tokenTranslationInformation = tokenTranslationInformation;
    }
}
