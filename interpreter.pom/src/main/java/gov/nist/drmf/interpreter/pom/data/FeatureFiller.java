package gov.nist.drmf.interpreter.pom.data;

import gov.nist.drmf.interpreter.pom.LineAnalyzer;
import gov.nist.drmf.interpreter.pom.MacrosLexicon;
import mlp.FeatureSet;

import java.util.stream.Stream;

public interface FeatureFiller extends InfoExtractor {
    Stream<FeatureFiller> allValues();

    default void fillFeatureSet(
            FeatureSet featureSet,
            LineAnalyzer lineAnalyzer,
            String cas
    ) {
        allValues().forEach( ff -> {
            String value = ff.getValue(cas, lineAnalyzer);
            if ( value != null && !value.isBlank() )
                featureSet.addFeature(ff.getKey(cas), value, MacrosLexicon.SIGNAL_INLINE);
        });
    }
}
