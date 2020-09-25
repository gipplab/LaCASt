package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.cas.constraints.Constraints;
import gov.nist.drmf.interpreter.cas.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.mlp.FeatureSetUtility;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.mlp.extensions.MatchablePomTaggedExpression;
import gov.nist.drmf.interpreter.mlp.extensions.PomMatcher;
import gov.nist.drmf.interpreter.mlp.extensions.PrintablePomTaggedExpression;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class Case {
    private static final Logger LOG = LogManager.getLogger(Case.class.getName());

    private String LHS, RHS;
    private Relations relation;

    private CaseMetaData metaData;

    public Case( String LHS, String RHS, Relations relation, CaseMetaData metaData ){
        this.LHS = LHS;
        this.RHS = RHS;
        this.relation = relation;
        this.metaData = metaData;
    }

    public boolean isDefinition() {
        return metaData.isDefinition();
    }

    public String getLHS() {
        return LHS;
    }

    public String getRHS() {
        return RHS;
    }

    public int getLine() {
        return metaData.getLinenumber();
    }

    public String getDlmf() {
        if ( metaData.getLabel() == null ) return null;
        return metaData.getLabel().getHyperlink();
    }

    public String getEquationLabel() {
        if ( metaData.getLabel() == null ) return null;
        return metaData.getLabel().getLabel();
    }

    public Relations getRelation() {
        return relation;
    }

    public List<String> getConstraintVariables(IConstraintTranslator ae, String label) {
        try {
            String[] vars = metaData.getConstraints().getSpecialConstraintVariables();
            vars = ae.translateEachConstraint(vars, label);
            return new LinkedList<>(Arrays.asList(vars));
        } catch ( NullPointerException npe ){
            return null;
        }
    }

    public List<String> getConstraintValues() {
        try {
            String[] vals = metaData.getConstraints().getSpecialConstraintValues();
            return new LinkedList<>(Arrays.asList(vals));
        } catch ( NullPointerException npe ){
            return null;
        }
    }

    public Constraints getConstraintObject(){
        return metaData.getConstraints();
    }

    public List<String> getConstraints(IConstraintTranslator ae, String label ) {
        try {
            String[] cons = metaData.getConstraints().getTexConstraints();
            cons = ae.translateEachConstraint(cons, label);
            LOG.debug("Active constraints: " + Arrays.toString(cons));
            return new LinkedList<>(Arrays.asList(cons));
        } catch ( NullPointerException npe ) {
            LOG.debug("No additional constraints identified.");
            return new LinkedList<>();
        } catch ( Error | Exception e ){
            LOG.error("Unable to generate Constraints. Reason: " + e.getMessage());
            return new LinkedList<>();
        }
    }

    public boolean isEquation(){
        return relation.equals(Relations.EQUAL);
    }

    public String specialValueInfo(){
        Constraints con = metaData.getConstraints();
        if ( con == null ) return null;
        return con.toString();
    }

    public String getRawConstraint() {
        if ( metaData.getConstraints() != null )
            return Arrays.toString(metaData.getConstraints().getTexConstraints());
        else return null;
    }

    public void removeConstraint() {
        metaData.deleteConstraints();
    }

    public CaseMetaData getMetaData() {
        return metaData;
    }

    @Override
    public String toString(){
        String s = getLine() + ": " + LHS + " " + relation + " " + RHS + "; ";
        s += getDlmf();
        return s;
    }

    private boolean addZReplacement() {
        String tmp = LHS + RHS;
        Matcher z = Z_PATTERN.matcher(tmp);
        Matcher x = X_PATTERN.matcher(tmp);
        Matcher y = Y_PATTERN.matcher(tmp);

        return z.find() && (x.find() || y.find());
    }

    private static final Pattern Y_PATTERN = Pattern.compile("(?<!\\\\[A-Za-z]{0,30})x(.|$)");
    private static final Pattern X_PATTERN = Pattern.compile("(?<!\\\\[A-Za-z]{0,30})y(.|$)");
    private static final Pattern Z_PATTERN = Pattern.compile("(?<!\\\\[A-Za-z]{0,30})z(.|$)");

    public Case replaceSymbolsUsed(SymbolDefinedLibrary library) {
        if ( this instanceof AbstractEvaluator.DummyCase ) return this;
        if ( LHS == null || RHS == null || relation == null ) return this;

        LinkedList<SymbolTag> used = metaData.getSymbolsUsed();

        for ( SymbolTag use : used ) {
            try {
                recursiveReplaceSymbolUsed(library, use);
            } catch (Exception | Error e) {
                LOG.warn("Unable to perform definition replacements.", e);
            }
//
//            SymbolTag def = library.getSymbolDefinition(use.getId());
//            if ( def != null ) {


//                String repl = def.getDefinition().replace("\\","\\\\").replace("$", "\\$");
//                Matcher m = AbstractEvaluator.filterCases.matcher(repl);
//                if ( m.find() ) {
//                    LOG.info("Found symbol definition but it includes illegal characters and will not be replaced (" + m.group(0) + ").");
//                } else {
//                    LOG.info("Found symbol definition! Replacing " + use.getSymbol() + " by " + def.getDefinition());
//                    this.LHS = this.LHS.replaceAll(Pattern.quote(use.getSymbol()), repl);
//                    this.RHS = this.RHS.replaceAll(Pattern.quote(use.getSymbol()), repl);
//                }
//            }
        }

        this.LHS = TeXPreProcessor.preProcessingTeX(this.LHS, this.getEquationLabel());
        this.RHS = TeXPreProcessor.preProcessingTeX(this.RHS, this.getEquationLabel());

        if ( addZReplacement() ) {
            StringBuilder sbL = new StringBuilder(), sbR = new StringBuilder();
            Matcher mL = Z_PATTERN.matcher(LHS);
            Matcher mR = Z_PATTERN.matcher(RHS);

            while ( mL.find() ) {
                String gr = mL.group(1).equals("\\") ? "\\\\" : mL.group(1);
                mL.appendReplacement(sbL, "(x+y\\\\iunit)" + gr);
            }
            while ( mR.find() ) {
                String gr = mR.group(1).equals("\\") ? "\\\\" : mR.group(1);
                mR.appendReplacement(sbR, "(x+y\\\\iunit)" + gr);
            }

            LHS = mL.appendTail(sbL).toString();
            RHS = mR.appendTail(sbR).toString();
        }

        return this;
    }

    private void recursiveReplaceSymbolUsed(SymbolDefinedLibrary library, SymbolTag use) throws ParseException {
        if ( use == null ) return;

        SymbolTag def = library.getSymbolDefinition(use.getId());
        if ( def == null ) return;

        // at first, we replace the tag itself.
        replaceSingleTag(def);

        // however, second step, we do the same for the children
        CaseMetaData meta = def.getMetaData();
        if ( meta == null ) return;

        LinkedList<SymbolTag> innerTags = meta.getSymbolsUsed();
        if ( innerTags == null ) return;
        for ( SymbolTag tag : innerTags ) {
            recursiveReplaceSymbolUsed(library, tag);
        }
    }

    private static final Pattern NVAR_PATTERN = Pattern.compile("\\\\NVar\\{(.*?)}");

    private void replaceSingleTag(SymbolTag def) throws ParseException {
        if ( def.getMetaData() != null ) {
            this.metaData.addConstraints(def.getMetaData().getConstraints());
        }

        SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();
        PrintablePomTaggedExpression ppteLHS = mlp.parse(this.LHS, this.getEquationLabel());
        PrintablePomTaggedExpression ppteRHS = mlp.parse(this.RHS, this.getEquationLabel());

        String symbol = def.getSymbol();
        Matcher m = NVAR_PATTERN.matcher(symbol);
        StringBuilder sb = new StringBuilder();

        while ( m.find() ) {
            int n = m.group(1).hashCode();
            m.appendReplacement(sb, "var"+n);
        }

        m.appendTail(sb);

        MatchablePomTaggedExpression matchPOM = new MatchablePomTaggedExpression(mlp, sb.toString(), "var\\d+");
        PomMatcher matcherL = matchPOM.matcher(ppteLHS);
        PrintablePomTaggedExpression left = matcherL.replaceAll("("+def.getDefinition()+")");
        this.LHS = left.getTexString();

        PomMatcher matcherR = matchPOM.matcher(ppteRHS);
        PrintablePomTaggedExpression right = matcherR.replaceAll("("+def.getDefinition()+")");
        this.RHS = right.getTexString();
    }

    public static boolean isSemantic(String symbol) {
        SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();
        try {
            PomTaggedExpression pte = mlp.parse(symbol);
            if ( !pte.getComponents().isEmpty() ) {
                pte = pte.getComponents().get(0);
            }

            MathTerm mt = pte.getRoot();
            FeatureSet fset = mt.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);

            return fset != null;
        } catch (ParseException e) {
            return false;
        }
    }
}
