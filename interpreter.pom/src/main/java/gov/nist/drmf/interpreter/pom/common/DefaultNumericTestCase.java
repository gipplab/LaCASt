package gov.nist.drmf.interpreter.pom.common;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.cas.Constraints;
import gov.nist.drmf.interpreter.common.eval.INumericTestCase;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Andre Greiner-Petter
 */
public class DefaultNumericTestCase implements INumericTestCase {
    private static final Logger LOG = LogManager.getLogger(DefaultNumericTestCase.class.getName());

    private final CaseMetaData metaData;

    private boolean equation = false;

    public DefaultNumericTestCase(TranslationInformation translationInformation) {
        metaData = CaseMetaData.extractMetaData(
                new LinkedList<>(translationInformation.getTranslatedConstraints()),
                new LinkedList<>(),
                null,
                -1
        );

    }

    @Override
    public List<String> getConstraints(IConstraintTranslator translator, String label) {
        return translateArray(translator, Constraints::getTexConstraints);
    }

    @Override
    public List<String> getConstraintVariables(IConstraintTranslator translator, String label) {
        return translateArray(translator, Constraints::getSpecialConstraintVariables);
    }

    @Override
    public List<String> getConstraintValues() {
        if ( metaData == null || metaData.getConstraints() == null || metaData.getConstraints().getSpecialConstraintVariables() == null )
            return new LinkedList<>();
        return List.of(metaData.getConstraints().getSpecialConstraintValues());
    }

    private List<String> translateArray(IConstraintTranslator translator, Function<Constraints, String[]> map) {
        try {
            Constraints c = metaData.getConstraints();
            String[] translations = translator.translateEachConstraint( map.apply(c) );
            return List.of(translations);
        } catch ( Exception e ) {
            LOG.debug("Unable to translate constraints. " + e.getMessage());
            return new LinkedList<>();
        }
    }
}
