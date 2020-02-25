package gov.nist.drmf.interpreter.evaluation.constraints;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class Constraints {

    private String[] texConstraints;

    private String[] specialConstraintVariables;
    private String[] specialConstraintValues;

    public Constraints(String[] texConstraints, String[] specialConstraintVariables, String[] specialConstraintValues){
        this.texConstraints = texConstraints;
        this.specialConstraintValues = specialConstraintValues;
        this.specialConstraintVariables = specialConstraintVariables;
    }

    public String[] getTexConstraints() {
        return texConstraints;
    }

    public String[] getSpecialConstraintVariables() {
        return specialConstraintVariables;
    }

    public String[] getSpecialConstraintValues() {
        return specialConstraintValues;
    }

    public String specialValuesInfo(){
        String s = "";

        if ( specialConstraintVariables != null ){
            s += "Set single values for variables (because of constraint-rules): ";
            for ( int i = 0; i < specialConstraintVariables.length; i++ ){
                s += specialConstraintVariables[i] + "=" + specialConstraintValues[i] + "; ";
            }
        }

        return s;
    }

    public String constraintInfo(){
        if ( texConstraints != null ){
            return "Applied Additional Constraints: " + Arrays.toString(texConstraints);
        } else return "No Constraints applied.";
    }

    @Override
    public String toString(){
        String s = "";

        if ( specialConstraintVariables != null ){
            s += "Set single values for variables (because of constraint-rules): ";
            for ( int i = 0; i < specialConstraintVariables.length; i++ ){
                s += specialConstraintVariables[i] + "=" + specialConstraintValues[i] + "; ";
            }
        }

        if ( texConstraints != null ){
            s += "Applied Additional Constraints: " + Arrays.toString(texConstraints);
        }

        if ( s.isEmpty() ) s = "No Constraints.";

        return s;
    }

    private static final Pattern MATH_PATTERN = Pattern.compile("^\\$?([^$]*)\\$?$");

    public static String stripDollar(String in) {
        Matcher m = MATH_PATTERN.matcher(in);
        if ( m.matches() )
            return m.group(1);
        return in;
    }

    public static final Pattern SIMPLE_ASS_SPLITTER = Pattern.compile("(.*)([<=>]+)([^<=>]+)([<=>]+)(.*)");

    public static String splitMultiAss(String ass) {
        Matcher m = SIMPLE_ASS_SPLITTER.matcher(ass);
        if ( m.matches() ) {
            return m.group(1) + m.group(2) + m.group(3) + ", " + m.group(3) + m.group(4) + m.group(5);
        }
        return ass;
    }

}
