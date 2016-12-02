package gov.nist.drmf.interpreter.common.grammar;

import gov.nist.drmf.interpreter.common.Keys;
import mlp.FeatureSet;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * @author Andre Greiner-Petter
 */
public interface IFeatureExtractor {
    String getFeatureValue(FeatureSet feature_set);

    static String getStringFromSet( SortedSet<String> set ){
        if ( set == null ) return "";
        String output = "";
        List<String> list = new ArrayList<>(set);
        while ( list.size() > 1 ){
            output += list.remove(0) + Keys.ALTERNATIVE_SPLIT;
        }
        output += list.remove(0);
        return output;
    }
}
