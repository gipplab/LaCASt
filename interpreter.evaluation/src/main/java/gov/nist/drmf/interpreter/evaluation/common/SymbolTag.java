package gov.nist.drmf.interpreter.evaluation.common;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolTag {
    // id
    private final String id;

    // symbol that gets defined
    private String symbol;

    // might be null
    private String definition;

    private CaseMetaData metaData;

    public SymbolTag(String id, String symbol) {
        this(id, symbol, null, null);
    }

    SymbolTag(String id, String symbol, String expr, CaseMetaData definitionMetaData ) {
        this.id = id;
        this.symbol = symbol;
        this.definition = expr;
        this.metaData = definitionMetaData;
    }

    public String getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDefinition() {
        return definition;
    }

    public CaseMetaData getMetaData() {
        return metaData;
    }
}
