package gov.nist.drmf.interpreter.cas.blueprints;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
public class BlueprintMaster {
    private static final Logger LOG = LogManager.getLogger(BlueprintMaster.class.getName());

    @Language("RegExp")
    private static final String WILDCARD_PATTERN = "(num[LU]|var)\\d+";

    public static final boolean LIMITED = false;
    public static final boolean LIM = true;

    private LinkedList<BlueprintRuleMatcher> limitedTrees;
    private LinkedList<BlueprintLimTree> limTrees;
    private SemanticLatexTranslator slt;

    private Path limitBTFile, limBTFile;

    public BlueprintMaster(SemanticLatexTranslator slt) {
        limitBTFile = GlobalPaths.PATH_MEOM_BLUEPRINTS;
        limBTFile = GlobalPaths.PATH_MEOM_LIMIT_BLUEPRINTS;
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
            limitedTrees.add(new BlueprintRuleMatcher(slt, s[0], s[1]));
        } catch (ParseException e) {
            LOG.error("Illegal string generating Blueprint: " + l, e);
        }
    }

    private void addLimTrees( String l ) {
        String[] s = l.split(" ==> ");
        try {
            limTrees.add(new BlueprintLimTree(slt, s[0], s[1]));
        } catch (ParseException e) {
            LOG.error("Illegal string for generating Blueprint: " + l, e);
        }
    }

    public MathematicalEssentialOperatorMetadata findMatchingLimit(boolean lim, String limit) {
        for ( BlueprintRuleMatcher t : (lim == LIM) ? limTrees : limitedTrees ) {
            if ( t.match(limit) ) return t.getExtractedMEOM();
        }
        return null;
    }

    public MathematicalEssentialOperatorMetadata findMatchingLimit(boolean lim, PomTaggedExpression... pte) {
        for ( BlueprintRuleMatcher t : (lim == LIM) ? limTrees : limitedTrees ) {
            if ( t.match(pte) ) return t.getExtractedMEOM();
        }
        return null;
    }
}
