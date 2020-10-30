package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.cas.constraints.Constraints;
import gov.nist.drmf.interpreter.cas.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.extensions.MatchablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.extensions.PomMatcher;
import gov.nist.drmf.interpreter.pom.extensions.PomMatcherBuilder;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class Case {
    private static final Logger LOG = LogManager.getLogger(Case.class.getName());

    private String originalLaTeXInput = null;

    private String LHS, RHS;
    private Relations relation;

    private CaseMetaData metaData;

    public Case( String LHS, String RHS, Relations relation, CaseMetaData metaData ){
        this.LHS = LHS;
        this.RHS = RHS;
        this.relation = relation;
        this.metaData = metaData;
    }

    public void setOriginalLaTeXInput(String formula) {
        this.originalLaTeXInput = formula;
    }

    public String getOriginalFormula() {
        return this.originalLaTeXInput;
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
        Set<String> memory = new HashSet<>();

        for ( SymbolTag use : used ) {
            try {
                recursiveReplaceSymbolUsed(library, use, memory);
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

    private void recursiveReplaceSymbolUsed(SymbolDefinedLibrary library, SymbolTag use, Set<String> memory) throws ParseException {
        if ( use == null ) return;

        SymbolTag def = library.getSymbolDefinition(use.getId());
        if ( def == null ) return;

        // to avoid infinity loops
        if ( memory.contains(def.getSymbol()) ) return;

        // at first, we replace the tag itself.
        replaceSingleTag(def);

        // remember the definitions we already performed
        memory.add(def.getSymbol());

        // however, second step, we do the same for the children
        CaseMetaData meta = def.getMetaData();
        if ( meta == null ) return;

        LinkedList<SymbolTag> innerTags = meta.getSymbolsUsed();
        if ( innerTags == null ) return;
        for ( SymbolTag tag : innerTags ) {
            recursiveReplaceSymbolUsed(library, tag, memory);
        }
    }

    private static final Pattern NVAR_PATTERN = Pattern.compile("\\\\NVar\\{(.*?)}");

    /**
     * Problem: Line 3933 use: \symbolUsed[\EulerGamma@{\NVar{z}}]{C5.S2.E1.m2badec}
     * But contains: \EulerGamma@{\nu+\tfrac{1}{2}}}
     * Now, C5.S2.E1.m2badec is defined in \symbolDefined[\EulerGamma@{\NVar{z}}]{C5.S2.E1.m2bdec}
     * with constraint: \constraint{\realpart@@{z}>0}
     *
     * Now it comes, when adding the constraint \realpart@@{z}>0, z must be replaced by \nu+\tfrac{1}{2}
     * Jesus...
     *
     * @param def
     * @throws ParseException
     */
    private void replaceSingleTag(SymbolTag def) throws ParseException {
        SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();
        PrintablePomTaggedExpression ppteLHS = mlp.parse(TeXPreProcessor.resetNumberOfAtsToOne(this.LHS), this.getEquationLabel());
        PrintablePomTaggedExpression ppteRHS = mlp.parse(TeXPreProcessor.resetNumberOfAtsToOne(this.RHS), this.getEquationLabel());

        String symbol = def.getSymbol();
        boolean isSemantic = isSemantic(symbol);
        Matcher m = NVAR_PATTERN.matcher(symbol);
        StringBuilder sb = new StringBuilder();

        int counter = 0;
        while ( m.find() ) {
            if ( m.group(1) != null ) {
                m.appendReplacement(sb, "VAR"+counter);
                counter++;
            }
        }

        m.appendTail(sb);

        MatchablePomTaggedExpression matchPOML = PomMatcherBuilder.compile(mlp, TeXPreProcessor.resetNumberOfAtsToOne(sb.toString()), "VAR\\d+");
        PomMatcher matcherL = matchPOML.matcher(ppteLHS);
        if ( counter == 0 && !isSemantic )
            updateLR(matcherL, def, true);

        MatchablePomTaggedExpression matchPOMR = PomMatcherBuilder.compile(mlp, TeXPreProcessor.resetNumberOfAtsToOne(sb.toString()), "VAR\\d+");
        PomMatcher matcherR = matchPOMR.matcher(ppteRHS);
        if ( counter == 0 && !isSemantic )
            updateLR(matcherR, def, false);

        if ( counter > 0 ) {
            LOG.trace("Auto definition replacement of semantic macros is suppressed. Macro " + symbol + " will not be replaced.");
            // now the fun part
            replaceConstraints(def, matcherL, matcherR);
        } else if ( def.getMetaData() != null ) {
            // if there was no NVar, simply add all constraints
            this.metaData.addConstraints(def.getMetaData().getConstraints());
        }
    }

    private void updateLR(PomMatcher matcher, SymbolTag def, boolean left) throws ParseException {
        PrintablePomTaggedExpression p = matcher.replacePattern("("+def.getDefinition()+")");
        if ( left ) this.LHS = p.getTexString();
        else this.RHS = p.getTexString();
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

    private void replaceConstraints(SymbolTag def, PomMatcher... matchers) {
        CaseMetaData defMetaData = def.getMetaData();
        if ( defMetaData == null ) return;

        Constraints consts = defMetaData.getConstraints();
        if ( consts == null ) return;

        Map<Integer, String> varSlots = defMetaData.getVariableSlots();
        if ( varSlots.isEmpty() ) return;

        Map<String, List<String>> varMapping = new HashMap<>();
        for ( PomMatcher m : matchers ) {
            m.reset();
            while ( m.find() ) {
                Map<String, String> groups = m.groups();
                for (Map.Entry<Integer, String> entry : varSlots.entrySet() ) {
                    String hit = groups.get("VAR"+entry.getKey());
                    if ( hit != null && !hit.isBlank() ) {
                        varMapping.computeIfAbsent(entry.getValue(), key -> new LinkedList<>()).add(hit);
                    }
                }
            }
        }

        LinkedList<String> newConstraints = new LinkedList<>();
        List<String> originalConstraints = consts.getOriginalConstraints();

        for ( String constraint : originalConstraints ) {
            // lets do simple string replacement, otherwise it might be an overkill... not?
            for ( Map.Entry<String, List<String>> varMap : varMapping.entrySet() ) {
                Pattern p = Pattern.compile("(?<!\\\\[A-Za-z]{0,30})"+Pattern.quote(varMap.getKey())+"(.|$)");
                Matcher m = p.matcher(constraint);
                if ( !m.find() ) continue;

                List<String> replacements = varMap.getValue();
                for ( String repl : replacements ) {
                    m = p.matcher(constraint);
                    StringBuilder sb = new StringBuilder();
                    repl = repl.replace("\\", "\\\\");

                    while ( m.find() ) {
                        m.appendReplacement(sb, repl + m.group(1));
                    }

                    m.appendTail(sb);
                    newConstraints.add(sb.toString());
                }
            }
        }

        // now its getting funny and a bit overkill. but anyway...
        CaseMetaData newMetaData = CaseMetaData.extractMetaData(
                newConstraints,
                defMetaData.getSymbolsUsed(),
                null,
                -1
        );

        this.metaData.addConstraints(newMetaData.getConstraints());
    }
}
