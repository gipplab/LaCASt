package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.cas.constraints.Constraints;
import gov.nist.drmf.interpreter.cas.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.mlp.FeatureSetUtility;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
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
            return new LinkedList<>(Arrays.asList(cons));
        } catch ( NullPointerException npe ){
            return null;
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

    public Case replaceSymbolsUsed(SymbolDefinedLibrary library) {
        LinkedList<SymbolTag> used = metaData.getSymbolsUsed();
        for ( SymbolTag use : used ) {
            SymbolTag def = library.getSymbolDefinition(use.getId());
            if ( def != null ) {
                String repl = def.getDefinition().replace("\\","\\\\").replace("$", "\\$");
                Matcher m = AbstractEvaluator.filterCases.matcher(repl);
                if ( m.find() ) {
                    LOG.info("Found symbol definition but it includes illegal characters and will not be replaced (" + m.group(0) + ").");
                } else {
                    LOG.info("Found symbol definition! Replacing " + use.getSymbol() + " by " + def.getDefinition());
                    this.LHS = this.LHS.replaceAll(Pattern.quote(use.getSymbol()), repl);
                    this.RHS = this.RHS.replaceAll(Pattern.quote(use.getSymbol()), repl);
                }
            }
        }
        return this;
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
