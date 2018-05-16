package gov.nist.drmf.interpreter.constraints;

import java.util.Arrays;

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
}
