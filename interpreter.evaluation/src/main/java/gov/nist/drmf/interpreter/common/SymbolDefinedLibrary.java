package gov.nist.drmf.interpreter.common;

import gov.nist.drmf.interpreter.evaluation.CaseMetaData;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolDefinedLibrary {
    private static final Pattern CLIP_PATTERN = Pattern.compile("(.*)\\..*$");

    public HashMap<String, SymbolTag> library;

    public SymbolDefinedLibrary() {
        this.library = new HashMap<>();
    }

    public void add(String id, String symbol, String definition, CaseMetaData metaData) {
        String normID = SymbolDefinedLibrary.clipID(id);
        SymbolTag sd = new SymbolTag(normID, symbol, definition, metaData);
        library.put(normID, sd);
    }

    public SymbolTag getSymbolDefinition( String id ) {
        String normID = SymbolDefinedLibrary.clipID(id);
        return library.get(normID);
    }

    public static String clipID( String id ) {
        Matcher m = CLIP_PATTERN.matcher(id);
        if ( m.matches() ) return m.group(1);
        return null;
    }
}
