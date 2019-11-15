package gov.nist.drmf.interpreter.cas.blueprints;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.grammar.LimDirections;
import mlp.ParseException;

/**
 * @author Andre Greiner-Petter
 */
public class BlueprintLimTree extends BlueprintLimitTree {
    private static final String DEFAULT_LOWER_LIMIT = "numL1";

    private LimDirections direction;

    public BlueprintLimTree(String blueprint,
                            String limitDir,
                            SemanticLatexTranslator translator) throws ParseException {
        super(blueprint, DEFAULT_LOWER_LIMIT, translator);
        this.direction = LimDirections.getDirection(limitDir);
    }

    @Override
    protected Limits getExtractedLimits() {
        Limits lim = super.getExtractedLimits();
        lim.setDirection(direction);
        return lim;
    }
}
