package gov.nist.drmf.interpreter.pom.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolTag {
    public static Pattern NVAR_PATTERN = Pattern.compile("\\\\NVar\\{(.*?)}");

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

    public SymbolTag(String id, String symbol, String expr, CaseMetaData definitionMetaData ) {
        this.id = id;
        this.symbol = symbol;
        this.definition = expr;
        this.metaData = definitionMetaData;
        analyzeNVar();
    }

    private void analyzeNVar() {
        if ( metaData == null ) return;
        int counter = 0;
        Matcher m = NVAR_PATTERN.matcher(symbol);
        while ( m.find() ) {
            metaData.addVariableSlot(counter, m.group(1));
            counter++;
        }
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
