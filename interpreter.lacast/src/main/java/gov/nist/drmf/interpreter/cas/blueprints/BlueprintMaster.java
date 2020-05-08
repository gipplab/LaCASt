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
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class BlueprintMaster {
    private static final Logger LOG = LogManager.getLogger(BlueprintMaster.class.getName());

    public static final boolean LIMITED = false;
    public static final boolean LIM = true;

    private LinkedList<BlueprintLimitTree> limitedTrees;
    private LinkedList<BlueprintLimTree> limTrees;
    private SemanticLatexTranslator slt;

    private Path limitBTFile, limBTFile;

    public BlueprintMaster(SemanticLatexTranslator slt) {
//        Path constraintsBT = GlobalPaths.PATH_LIBS.resolve("blueprints.txt");
        limitBTFile = GlobalPaths.PATH_LIMITED_BLUEPRINTS;
        limBTFile = GlobalPaths.PATH_LIMES_BLUEPRINTS;
        limitedTrees = new LinkedList<>();
        limTrees = new LinkedList<>();
        this.slt = slt;
    }

    public void init() throws IOException {
        Files.readAllLines(limitBTFile)
                .forEach( this::addLimitedTrees );

        Files.readAllLines(limBTFile)
                .forEach( this::addLimTrees );
    }

    private void addLimitedTrees( String l ) {
        String[] s = l.split(" ==> ");
        try {
            limitedTrees.add(new BlueprintLimitTree(s[0], s[1], slt));
        } catch (ParseException | IOException e) {
            LOG.error("Illegal string generating Blueprint: " + l, e);
        }
    }

    private void addLimTrees( String l ) {
        String[] s = l.split(" ==> ");
        try {
            limTrees.add(new BlueprintLimTree(s[0], s[1], slt));
        } catch (ParseException | IOException e) {
            LOG.error("Illegal string for generating Blueprint: " + l, e);
        }
    }

    public Limits findMatchingLimit(boolean lim, String limit) {
        for ( BlueprintLimitTree t : (lim == LIM) ? limTrees : limitedTrees ) {
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

    public Limits findMatchingLimit(boolean lim, PomTaggedExpression... pte) {
        for ( BlueprintLimitTree t : (lim == LIM) ? limTrees : limitedTrees ) {
            try {
                if ( t.matches(pte) ) return t.getExtractedLimits();
            } catch (IllegalArgumentException iae) {
                LOG.error(iae.getMessage(), iae);
                return null;
            }
        }
        return null;
    }
}
