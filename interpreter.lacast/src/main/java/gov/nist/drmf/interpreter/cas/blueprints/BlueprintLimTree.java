package gov.nist.drmf.interpreter.cas.blueprints;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.pom.common.grammar.LimDirections;
import mlp.ParseException;

/**
 * @author Andre Greiner-Petter
 */
public class BlueprintLimTree extends BlueprintRuleMatcher {
    private static final String DEFAULT_LOWER_LIMIT = "numL1";

    private LimDirections direction;

    public BlueprintLimTree(SemanticLatexTranslator translator, String blueprint,
                            String limitDir) throws ParseException {
        super(translator, blueprint, DEFAULT_LOWER_LIMIT);
        this.direction = LimDirections.getDirection(limitDir);
    }

    @Override
    protected MathematicalEssentialOperatorMetadata getExtractedMEOM() {
        MathematicalEssentialOperatorMetadata lim = super.getExtractedMEOM();
        lim.setDirection(direction);
        return lim;
    }
}
