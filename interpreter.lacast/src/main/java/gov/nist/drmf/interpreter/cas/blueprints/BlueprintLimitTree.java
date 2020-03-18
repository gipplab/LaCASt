package gov.nist.drmf.interpreter.cas.blueprints;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.mlp.extensions.FakeMLPGenerator;
import gov.nist.drmf.interpreter.mlp.extensions.FeatureSetUtility;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class BlueprintLimitTree {
    private static final Logger LOG = LogManager.getLogger(BlueprintLimitTree.class.getName());

    private static final String VAR_SPLITTER = " / ";
    private static final String LOW_UP_SPLITTER = ",";

    private String varPattern, upBPattern, lowBPattern;
    private final Pattern LIMIT_PATTERN;

    private BlueprintLimitNode root;
    private MLPWrapper mlp;

    private LinkedList<BlueprintLimitNode> vars, lowerLimits, upperLimits;
    private String[] limitPattern;

    private SemanticLatexTranslator translator;

    private boolean isOverSet = false;

    public BlueprintLimitTree(
            String blueprint,
            String limitPattern,
            SemanticLatexTranslator translator
    ) throws ParseException, IOException {
        blueprint = preCleaning(blueprint);
        this.mlp = new SemanticMLPWrapper();
        PomTaggedExpression topExpr = mlp.parse(blueprint);
        this.root = createBlueprint(topExpr);
        resetMatch();
        this.limitPattern = limitPattern.split(VAR_SPLITTER);
        this.translator = translator;
        this.LIMIT_PATTERN = setupPattern();
    }

    private Pattern setupPattern() {
        lowBPattern = translate(BlueprintLimitNode.LOWER_BOUND_TOKEN);
        upBPattern = translate(BlueprintLimitNode.UPPER_BOUND_TOKEN);
        varPattern = translate(BlueprintLimitNode.VAR_TOKEN);

        lowBPattern = lowBPattern.replaceAll("\\*", "\\\\*");
        upBPattern = upBPattern.replaceAll("\\*", "\\\\*");
        varPattern = varPattern.replaceAll("\\*", "\\\\*");

        return Pattern.compile(
                "("+lowBPattern+"|"+varPattern+"|"+upBPattern+").?(\\d+)"
        );
    }

    private BlueprintLimitNode createBlueprint(PomTaggedExpression blueprint) {
        MathTerm term = blueprint.getRoot();

        if (term == null || term.isEmpty()) {
            // its a sequence, first analyze all kids
            List<PomTaggedExpression> list = blueprint.getComponents();
            LinkedList<BlueprintLimitNode> kids = new LinkedList<>();

            for (PomTaggedExpression pte : list) {
                kids.add(createBlueprint(pte));
            }

            return new BlueprintLimitNode(this, kids, blueprint);
        } else { // term is a single node!
            String tag = term.getTag();
            if (FeatureSetUtility.isGreekLetter(term)) {
                tag = BlueprintLimitNode.GREEK_TAG;
            }

            // its a sequence, first analyze all kids
            List<PomTaggedExpression> list = blueprint.getComponents();
            LinkedList<BlueprintLimitNode> kids = new LinkedList<>();
            if ( list != null && !list.isEmpty() ) {
                for (PomTaggedExpression pte : list) {
                    kids.add(createBlueprint(pte));
                }
                return new BlueprintLimitNode(this, kids, blueprint, term.getTermText(), tag);
            } else {
                return new BlueprintLimitNode(
                        this,
                        term.getTermText(),
                        tag,
                        blueprint
                );
            }

        }
    }

    protected void overSet(boolean isOverSet) {
        this.isOverSet = isOverSet;
    }

    protected void addLowerLimit(BlueprintLimitNode lowerLimit, int idx) {
        this.lowerLimits.add(idx-1, lowerLimit);

    }

    protected void addUpperLimit(BlueprintLimitNode upperLimit, int idx) {
        this.upperLimits.add(idx-1, upperLimit);
    }

    protected void addVar(BlueprintLimitNode var) {
        this.vars.add(var);
    }

    protected void addVar(BlueprintLimitNode var, int idx) {
        while ( idx > this.vars.size()+1 ) {
            this.vars.addLast(new BlueprintLimitNode());
        }

        if ( idx == this.vars.size() ) {
            this.vars.addLast(var);
        } else {
            this.vars.add(idx-1, var);
        }
    }

    protected Limits getExtractedLimits(){
        LinkedList<String> variables = new LinkedList<>();
        LinkedList<String> lowers = new LinkedList<>();
        LinkedList<String> uppers = new LinkedList<>();

        for ( int i = 0; i < this.vars.size(); i++ ) {
            String[] lu;
            if ( i >= limitPattern.length ) {
                lu = limitPattern[limitPattern.length-1].split(LOW_UP_SPLITTER);
            } else {
                lu = limitPattern[i].split(LOW_UP_SPLITTER);
            }

            String t = translate(lu[0]);
            Matcher lMatcher = LIMIT_PATTERN.matcher(t); // lower limit
            lowers.addLast(replaceAllPatterns(lMatcher));

            if ( lu.length == 2 ) {
                t = translate(lu[1]);
                Matcher uMatcher = LIMIT_PATTERN.matcher(t); // upper limit
                uppers.addLast(replaceAllPatterns(uMatcher));
            } else {
                uppers.addLast(translate(Limits.DEFAULT_UPPER_LIMIT));
            }

            BlueprintLimitNode v = vars.get(i);
            t = translate(v.getMLPNode());
            variables.addLast(v.getPrefix()+t);
        }

        Limits l = new Limits(variables, lowers, uppers);
        l.setLimitOverSet(isOverSet);
        return l;
    }

    protected String translate(PomTaggedExpression pte) {
        translator.translate(pte);
        return translator.getTranslatedExpression();
    }

    protected String translate(String str) {
        translator.translate(str);
        return translator.getTranslatedExpression();
    }

    private String replaceAllPatterns( Matcher matcher ) {
        StringBuffer buffer = new StringBuffer();
        while( matcher.find() ){
            if ( matcher.group(1).matches(lowBPattern) ) {
                // lower index replacement
                int idx = Integer.parseInt(matcher.group(2));
                BlueprintLimitNode l = this.lowerLimits.get(idx-1);
                String trans = l.getPrefix() + translate(copy(l.getMLPNode()));
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(trans));
            } else if ( matcher.group(1).matches(upBPattern) ) {
                // lower index replacement
                int idx = Integer.parseInt(matcher.group(2));
                BlueprintLimitNode l = this.upperLimits.get(idx-1);
                String trans = l.getPrefix() + translate(copy(l.getMLPNode()));
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(trans));
            } else if ( matcher.group(1).matches(varPattern) ) {
                // lower index replacement
                int idx = Integer.parseInt(matcher.group(2));
                BlueprintLimitNode l = this.vars.get(idx-1);
                String trans = l.getPrefix() + translate(copy(l.getMLPNode()));
                matcher.appendReplacement(buffer, trans);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private void resetMatch() {
        vars = new LinkedList<>();
        lowerLimits = new LinkedList<>();
        upperLimits = new LinkedList<>();
        isOverSet = false;
    }

    private PomTaggedExpression copy(PomTaggedExpression in) {
        if ( in.getTag() != null && in.getTag().matches("sequence") ) {
            PomTaggedExpression c = FakeMLPGenerator.generateEmptySequencePTE();
            for ( PomTaggedExpression child : in.getComponents() ) {
                c.addComponent(child);
            }
            return c;
        }
        return in;
    }

    public boolean matches(String texLimit) throws ParseException {
        resetMatch();
        texLimit = preCleaning(texLimit);
        PomTaggedExpression topExpr = mlp.parse(texLimit);
        BlueprintLimitNode reference = createBlueprint(topExpr);
        return this.root.equals(reference);
    }

    public boolean matches(PomTaggedExpression... pte) {
        resetMatch();
        if ( pte == null ) throw new IllegalArgumentException("Null does not match any expression!");
        if ( pte.length > 1 ) {
            PomTaggedExpression top = FakeMLPGenerator.generateEmptySequencePTE();
            for ( PomTaggedExpression pt : pte ){
                top.addComponent(pt);
            }
            BlueprintLimitNode reference = createBlueprint(top);
            return this.root.equals(reference);
        } else if ( pte.length < 1 ) {
            throw new IllegalArgumentException("Cannot match empty limited expressions.");
        } else {
            BlueprintLimitNode reference = createBlueprint(pte[0]);
            return this.root.equals(reference);
        }
    }

    public static String preCleaning(String constraint){
        constraint = TeXPreProcessor.preProcessingTeX(constraint);
        constraint = constraint.replaceAll("\\\\[lc]?dots[cbmio]?", "\\\\dots");
        constraint = constraint.replaceAll("([^,\\s])\\s*\\\\dots", "$1, \\\\dots");
        constraint = constraint.replaceAll("\\\\[it]?frac", "\\\\frac");
        constraint = constraint.replaceAll("\\\\(ne|le|ge)([^a-zA-Z])", "\\\\$1q$2");
        constraint = constraint.replaceAll("[\\s,;.]*$", "");
        constraint = constraint.replaceAll("@*", "");
        return constraint;
    }
}
