package gov.nist.drmf.interpreter.semantic;

import java.util.ArrayList;

/**
 * Java class representing a Macro from a .sty file
 */
public class Macro {

    /**
     * String to be replaced
     */
    private String toReplace;

    /**
     * Replacement string of the macro
     */
    private String replacement;

    /**
     * Type of macro
     */
    private String type;

    /**
     * Number of arguments
     */
    private int argNum;

    /**
     * Number of parameters
     */
    private int paramNum;

    /**
     * Possible arrangements of the Macro
     */
    private ArrayList<String> arrangements;

    /**
     * Returns toReplace field
     * @return
     */
    public String getToReplace() {
        return toReplace;
    }

    /**
     * Returns replacement field
     * @return
     */
    public String getReplacement() {
        return replacement;
    }

    /**
     * Returns argNum field
     * @return
     */
    public int getArgNum() {
        return argNum;
    }

    /**
     * Returns paramNum field
     * @return
     */
    public int getParamNum() {
        return paramNum;
    }

    /**
     * Returns type field
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * Returns arrangements field
     * @return
     */
    public ArrayList<String> getArrangements() {
        return arrangements;
    }

    /**
     * Initializes a Macro object given an ArrayList of parameters
     * @param params
     */
    public Macro(ArrayList<String> params) {
        replacement = params.get(1);
        arrangements = new ArrayList<>();
        type = params.get(0);
        if (params.get(0).equals("newcommand") || params.get(0).equals("renewcommand") || params.get(0).equals("DeclareRobustCommand")) {
            toReplace = params.get(2);
            argNum = 0;
            paramNum = 0;
        } else {
            replacement = "\\" + replacement;
            if (Character.isDigit(params.get(2).charAt(0))) {
                argNum = Integer.parseInt(params.get(2));
                toReplace = params.get(3);
            } else {
                argNum = 0;
                toReplace = params.get(2);
            }
            int i = params.size() - 1;
            while (!Character.isDigit(params.get(i).charAt(0))) {
                i--;
            }
            paramNum = Integer.parseInt(params.get(i));
            while (i < params.size()-1) {
                i++;
                arrangements.add(params.get(i));
            }
        }
        System.out.println(type + " " + replacement + " " + toReplace + " " + argNum + " " + paramNum + " " + arrangements); //for testing
        System.out.println(params);
    }

}
