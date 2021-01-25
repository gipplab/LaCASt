package gov.nist.drmf.interpreter.generic.macro;

import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticReplacementRule;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class RetrievedMacros {

    private final MacroDistributionAnalyzer macroDistributionAnalyzer;

    private final Set<String> definiensMemory;
    private final Set<String> macroMemory;
    private final Set<String> visitedNodesCache;

    private final List<SemanticReplacementRule> macroPatterns;

    private boolean ordered = false;

    public RetrievedMacros(MacroDistributionAnalyzer macroDistributionAnalyzer) {
        this.macroDistributionAnalyzer = macroDistributionAnalyzer;
        this.definiensMemory = new HashSet<>();
        this.macroMemory = new HashSet<>();
        this.macroPatterns = new LinkedList<>();
        this.visitedNodesCache = new HashSet<>();
    }

    public boolean visitedNode(String id) {
        return visitedNodesCache.contains(id);
    }

    public boolean containsDefinition(String def) {
        return definiensMemory.contains(def);
    }

    public boolean containsMacro(String macro) {
        return macroMemory.contains(macro);
    }

    public void addNodeVisit(String id) {
        this.visitedNodesCache.add(id);
    }

    public void addDefinition(String def){
        this.definiensMemory.add(def);
    }

    public void addMacro(String macro) {
        this.macroMemory.add(macro);
    }

    public void addPattern(SemanticReplacementRule macroPattern) {
        this.macroPatterns.add(macroPattern);
        this.ordered = false;
    }

    public List<SemanticReplacementRule> getPatterns() {
        orderRules();
        return macroPatterns;
    }

    private void orderRules() {
        if ( ordered ) return;
        macroPatterns.sort((a, b) -> {
            double diff = b.getScore() - a.getScore();
            if ( diff == 0 ) {
                MacroCounter c1 = macroDistributionAnalyzer.getMacroCounter( "\\" + a.getMacro().getName() );
                MacroCounter c2 = macroDistributionAnalyzer.getMacroCounter( "\\" + b.getMacro().getName() );

                int counterDiff = c2.getMacroCounter() - c1.getMacroCounter();
                if ( counterDiff == 0 ) {
                    return b.getPattern().getGenericTex().length() - a.getPattern().getGenericTex().length();
                } else return counterDiff;
            }

            return Double.compare(b.getScore(), a.getScore());
        });
        ordered = true;
    }
}

