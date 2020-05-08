package gov.nist.drmf.interpreter.common.interfaces;

import gov.nist.drmf.interpreter.mlp.MacrosLexicon;
import mlp.FeatureSet;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * @author Andre Greiner-Petter
 */
public interface IFeatureExtractor {
    String getFeatureValue(FeatureSet feature_set, String cas);

    static String getStringFromSet( SortedSet<String> set ){
        if ( set == null ) return "";
        String output = "";
        List<String> list = new ArrayList<>(set);
        while ( list.size() > 1 ){
            output += list.remove(0) + MacrosLexicon.SIGNAL_INLINE;
        }
        output += list.remove(0);
        return output;
    }
}
