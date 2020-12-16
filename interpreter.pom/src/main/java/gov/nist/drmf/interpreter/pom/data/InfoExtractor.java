package gov.nist.drmf.interpreter.pom.data;

import gov.nist.drmf.interpreter.pom.LineAnalyzer;

public interface InfoExtractor {
    String getKey(String cas);

    boolean isMandatory();

    default String getValue(String cas, LineAnalyzer lineAnalyzer) throws NullPointerException {
        String key = getKey(cas);
        String value = lineAnalyzer.getValue(key);
        if ( isMandatory() && (value.isEmpty()) ) {
            throw new NullPointerException(this + " is a mandatory value which is missing");
        } else return value;
    }
}
