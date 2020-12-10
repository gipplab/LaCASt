package gov.nist.drmf.interpreter.common.cas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class Constraints {

    private final List<String> allConstraints;
    private final List<String> texConstraints;
    private final List<String> specialConstraintVariables;
    private final List<String> specialConstraintValues;

    public Constraints() {
        allConstraints = new ArrayList<>();
        texConstraints = new ArrayList<>();
        specialConstraintVariables = new ArrayList<>();
        specialConstraintValues = new ArrayList<>();
    }

    public Constraints(LinkedList<String> originalConstraintList, String[] texConstraints, String[] specialConstraintVariables, String[] specialConstraintValues){
        this.allConstraints = originalConstraintList;
        this.texConstraints = new LinkedList<>(Arrays.asList(texConstraints));
        this.specialConstraintValues = new LinkedList<>(Arrays.asList(specialConstraintValues));
        this.specialConstraintVariables = new LinkedList<>(Arrays.asList(specialConstraintVariables));
    }

    public List<String> getOriginalConstraints() {
        return allConstraints;
    }

    public String[] getTexConstraints() {
        return texConstraints.stream().distinct().toArray(String[]::new);
    }

    public String[] getSpecialConstraintVariables() {
        return specialConstraintVariables.stream().distinct().toArray(String[]::new);
    }

    public String[] getSpecialConstraintValues() {
        return specialConstraintValues.stream().distinct().toArray(String[]::new);
    }

    public void addConstraints(Constraints c) {
        if ( c == null ) return;
        this.allConstraints.addAll(c.allConstraints);
        this.texConstraints.addAll(c.texConstraints);
        this.specialConstraintVariables.addAll(c.specialConstraintVariables);
        this.specialConstraintValues.addAll(c.specialConstraintValues);
    }

    public String specialValuesInfo(){
        return generateConstraintString();
    }

    public String constraintInfo(){
        String s = addAdditionalConstraintString();
        return s==null ? "No Constraints applied." : s;
    }

    private String generateConstraintString() {
        String s = "";
        if ( specialConstraintVariables != null ){
            s += "Set single values for variables (because of constraint-rules): ";
            for ( int i = 0; i < specialConstraintVariables.size(); i++ ){
                s += specialConstraintVariables.get(i) + "=" + specialConstraintValues.get(i) + "; ";
            }
        }
        return s;
    }

    private String addAdditionalConstraintString() {
        if ( texConstraints != null )
            return "Applied Additional Constraints: " + texConstraints;
        else return null;
    }

    @Override
    public String toString(){
        String s = specialValuesInfo();

        String tmp = addAdditionalConstraintString();
        s += tmp == null ? "" : tmp;

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
