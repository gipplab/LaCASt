package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.components.SequenceTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Andre Greiner-Petter
 */
public class MacroDerivativeHelper {
    private static final Logger LOG = LogManager.getLogger(MacroDerivativeHelper.class.getName());

    private final AbstractTranslator abstractTranslator;
    private final String CAS;
    private final Function<String, TranslationException> generateException;

    public MacroDerivativeHelper(AbstractTranslator abstractTranslator, Function<String, TranslationException> generateException) {
        this.abstractTranslator = abstractTranslator;
        this.CAS = abstractTranslator.getConfig().getTO_LANGUAGE();
        this.generateException = generateException;
    }

    public TranslatedExpression getArgumentsBasedOnDiffVar(
            List<PomTaggedExpression> followingExps,
            List<String> vars,
            DerivativeAndPowerHolder diffPowerHolder
    ) {
        // otherwise! we have a problem similar to sums. When does the argument ends?
        // so lets follow sums approach
        PomTaggedExpression variablePTE = followingExps.remove(0);

        diffPowerHolder.setComplexDerivativeVar(!PomTaggedExpressionUtility.isSingleVariable(variablePTE));

        TranslatedExpression varTE = abstractTranslator.translateInnerExp(variablePTE, new LinkedList<>());

        vars.add(varTE.toString());

        List<PomTaggedExpression> potentialArgs = new LinkedList<>();
        try {
            potentialArgs = MeomArgumentExtractor.getPotentialArgumentsUntilEndOfScope(
                    followingExps,
                    vars,
                    abstractTranslator
            );
        } catch ( TranslationException te ) {
            if ( !TranslationExceptionReason.INVALID_LATEX_INPUT.equals(te.getReason()) ) throw te;
        }

        if ( !potentialArgs.isEmpty() ) {
            // the potential arguments is a theoretical sequence, so handle it as a sequence!
            PomTaggedExpression topSeqPTE = FakeMLPGenerator.generateEmptySequencePPTE();
            for ( PomTaggedExpression pte : potentialArgs ) topSeqPTE.addComponent(pte);

            SequenceTranslator p = new SequenceTranslator(abstractTranslator);
            return p.translate( topSeqPTE );
        } else return null;
    }

    public Set<String> extractVariableOfDiff(PomTaggedExpression exp) {
        if (PomTaggedExpressionUtility.isSequence(exp)) {
            return extractVariableOfDiff(exp.getComponents());
        }

        // ok in this case, its a single expression
        // now we have the good old identifier problem.
        MathTermTags tag = MathTermTags.getTagByExpression(exp);
        Set<String> set = new HashSet<>();
        switch (tag) {
            case alphanumeric:
            case special_math_letter:
            case symbol:
            case letter:
                set.add(exp.getRoot().getTermText());
        }
        return set;
    }

    private Set<String> extractVariableOfDiff(List<PomTaggedExpression> expressions) {
        Set<String> variableCandidates = new HashSet<>();
        for ( int i = 0; i < expressions.size(); i++ ) {
            PomTaggedExpression p = expressions.get(i);
            MathTerm mt = p.getRoot();

            if ( MathTermUtility.isDLMFMacro(mt) ) {
                i = handleMacro(mt, expressions, i, variableCandidates);
            } else {
                variableCandidates.addAll(extractVariableOfDiff(p));
            }

        }
        return variableCandidates;
    }

    private int handleMacro(MathTerm mt, List<PomTaggedExpression> expressions, int i, Set<String> variableCandidates) {
        FeatureSet fset = mt.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        Integer slot;
        Integer numberOfVariables;
        try {
            slot = Integer.parseInt(DLMFFeatureValues.SLOT_DERIVATIVE.getFeatureValue(fset, CAS));
            numberOfVariables = Integer.parseInt(DLMFFeatureValues.NUMBER_OF_VARIABLES.getFeatureValue(fset, CAS));
        } catch (NumberFormatException e) {
            LOG.warn("No slot of differentiation found for " + mt.getTermText() + ", assuming its 1.");
            slot = 1;
            numberOfVariables = 1;
        }

        try {
            i = skipAllAts(expressions, i);
            int idx = i + slot - 1; // the slot is 1 not 0, so we must add -1
            variableCandidates.addAll(extractVariableOfDiff(expressions.get(idx)));
            return i + numberOfVariables;
        } catch (IndexOutOfBoundsException e) {
            throw generateException.apply("Unable to find @ after a DLMF macro. That is invalid syntax");
        }
    }

    private int skipAllAts(List<PomTaggedExpression> list, int startIdx) {
        boolean passedAtYet = false;
        for ( int i = startIdx; i < list.size(); i++ ) {
            PomTaggedExpression tmp = list.get(i);
            boolean isAt = MathTermUtility.equals(tmp.getRoot(), MathTermTags.at);
            if ( isAt && !passedAtYet ) passedAtYet = true;
            else if ( !isAt && passedAtYet ) {
                // so passed ats and thats the first that is not an at... we found it
                return i;
            }
        }
        throw new IndexOutOfBoundsException();
    }

    public void updateSetOfCandidates(PomTaggedExpression exp, Set<String> variableCandidates) {
        if ( variableCandidates.isEmpty() ) {
            throw generateException.apply("Unable to extract variable of differentiation");
        }

        Set<String> set = extractVariableOfDiff(exp);
        variableCandidates.retainAll(set);
    }
}
