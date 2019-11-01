package gov.nist.drmf.interpreter.cas.blueprints;

import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
public class BlueprintMaster {
    private static final Logger LOG = LogManager.getLogger(BlueprintMaster.class.getName());

    private LinkedList<BlueprintLimitTree> constraintsBTTrees;
    private SemanticLatexTranslator slt;

    private Path limitBTFile;

    public BlueprintMaster(SemanticLatexTranslator slt) {
        Path constraintsBT = GlobalPaths.PATH_LIBS.resolve("blueprints.txt");
        limitBTFile = GlobalPaths.PATH_LIBS.resolve("limit-blueprints.txt");
        this.slt = slt;
    }

    public void init() throws IOException {
        constraintsBTTrees = new LinkedList<>();
        Files.readAllLines(limitBTFile)
                .stream()
                .forEach( l -> {
                    String[] s = l.split(" ==> ");
                    try {
                        constraintsBTTrees.add(new BlueprintLimitTree(s[0], s[1], slt));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                });
    }

    public Limits findMatchingLimit(String limit) {
        for ( BlueprintLimitTree t : constraintsBTTrees ) {
            try {
                if ( t.matches(limit) ) {
                    return t.getExtractedLimits();
                }
            } catch (ParseException e) {
                LOG.error("Illegal string for limit checks. " + limit, e);
                return null;
            }
        }
        return null;
    }

    public Limits findMatchingLimit(PomTaggedExpression... pte) {
        for ( BlueprintLimitTree t : constraintsBTTrees ) {
            if ( t.matches(pte) ) return t.getExtractedLimits();
        }
        return null;
    }
}
