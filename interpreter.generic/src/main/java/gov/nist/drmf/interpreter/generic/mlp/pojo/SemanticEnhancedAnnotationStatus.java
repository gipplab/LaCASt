package gov.nist.drmf.interpreter.generic.mlp.pojo;

public enum SemanticEnhancedAnnotationStatus {
    BASE(0, "base document without annotations or graph structure"),
    SEMANTICALLY_ANNOTATED(1, "semantic document with dependency graph and definiens annotations"),
    TRANSLATED(2, "semantic document including translations to semantic LaTeX and CAS representations"),
    COMPUTED(3, "full-fledged semantic computed document include dependency graph, translations, and CAS computations");

    private final int rank;
    private final String description;

    SemanticEnhancedAnnotationStatus(int rank, String description) {
        this.rank = rank;
        this.description = description;
    }

    public boolean hasPassed(SemanticEnhancedAnnotationStatus reference) {
        return reference.rank <= this.rank;
    }

    public int getRank() {
        return rank;
    }

    public String getDescription() {
        return description;
    }

    public SemanticEnhancedAnnotationStatus increaseRank() {
        if ( rank >= 2 ) return COMPUTED;
        else if ( rank == 1 ) return TRANSLATED;
        else if ( rank == 0 ) return SEMANTICALLY_ANNOTATED;
        else return BASE;
    }
}
