package gov.nist.drmf.interpreter.mlp.data;

import gov.nist.drmf.interpreter.mlp.LineAnalyzer;
import gov.nist.drmf.interpreter.mlp.MacrosLexicon;
import mlp.FeatureSet;

import java.util.Iterator;
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
