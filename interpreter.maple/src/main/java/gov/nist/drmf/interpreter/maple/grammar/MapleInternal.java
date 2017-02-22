package gov.nist.drmf.interpreter.maple.grammar;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a list of translatable internal objects of maple.
 * Most of the internal objects can be ignored or shouldn't come
 * up during our translation process.
 *
 * @see <a href="http://www.maplesoft.com/support/help/Maple/view.aspx?path=ProgrammingGuide/Appendix1">Maple Internal Representation</a>
 * Created by AndreG-P on 21.02.2017.
 */
public enum MapleInternal {
    /**
     * Summations and products
     * expression sequences are a special kind of lists.
     */
    sum("SUM"),             // [SUM, summand_1, summand_2, ...]
    prod("PROD"),           // [PROD, exp1, exp1_exponent, exp2, exp2_exponent, ...]
    exp("EXPSEQ"),          // [EXPSEQ, exp_1, exp_2, ...], usually represented a list, set or function calls

    /**
     * Numerical expressions are always leafs!
     */
    intpos("INTPOS"),       // [INTPOS, integer]
    intneg("INTNEG"),       // [INTNEG, integer]
    complex("COMPLEX"),     // [COMPLEX, RE, IM] OR [COMPLEX, IM]
    floating("FLOAT"),      // [FLOAT, integer_1, integer_2], means integer_1*10^integer_2
    rational("RATIONAL"),   // [RATIONAL, integer, pos_integer], 5/(-2) is [RATIONAL, -5, 2]

    /**
     * Simple and complex computations
     */
    power("POWER"),         // [POWER, exp1, exp2] exp1^exp2
    function("FUNCTION"),   // [FUNCTION, [ASSIGNEDNAME, "func_name", ...], [EXPSEQ, ...]]

    /**
     * ASSIGNEDNAME is a special variant of NAME. Usually it contains the name of a function.
     * For instance cos(2) is
     *  [FUNCTION, [ASSIGNEDNAME, "name", "PROC", [attributes...]], [EXPSEQ, [arg1], [arg2]]
     * But it could be also a usual NAME with extra information
     *  [ASSIGNEDNAME, "a", "INTPOS"] if previously a was defined to be a positive integer.
     *
     * NAME is just a simple name of a variable.
     * STRING is a simple string
     */
    ass_name("ASSIGNEDNAME"),
    name("NAME"),           // [NAME, variable or something else as String]
    string("STRING"),       // [STRING, "string"]

    /**
     * Relations! Always contains two expressions.
     */
    equation("EQUATION"),   // [EQUATION, exp1, exp2] exp1 = exp2
    ineq("INEQUAT"),        // [INEQAT, exp1, exp2] <>
    lesseq("LESSEQ"),       // [LESSEQ, exp1, exp2] >= or <=
    lessthan("LESSTHAN"),   // [LESSTHAN, exp1, exp2] > or <
    imply("IMPLIES"),       // [IMPLIES, exp1, exp2] =>

    /**
     * Logical operators
     */
    not("NOT"),             // [NOT, exp] logical not
    or("OR"),               // [OR, exp1, exp2] exp1 OR exp2
    xor("XOR"),             // [XOR, exp1, exp2] exp1 XOR exp2

    /**
     * Sets
     */
    set("SET"),             // [SET, [EXPSEQ, ...], optional attributes] {exp1, exp2, ...}

    /**
     * TODO problematics:
     * Maybe we should support these as well.
     */
    //list("LIST"),           // [LIST, [EXPSEQ, ...]]
    //poly("POLY"),           // ...
    //range("RANGE"),         // [RANGE, exp1, exp2] exp1 .. exp2
    //statseq("STATSEQ"),     // [STATSEQ, stat1, stat2, ...] stat1; stat2; ...
    ;

    /**
     * Really handy "initialization-on-demand holder idiom".
     */
    private static class Holder {
        static Map<String, MapleInternal> INTERNAL_MAP = new HashMap<>();
    }

    /**
     * The name of an internal maple object.
     */
    private final String id;

    /**
     * Initialize the enum values and put them into a static hash-map to make it fast to find.
     * @param id name of internal maple object
     */
    MapleInternal( String id ){
        this.id = id;
        Holder.INTERNAL_MAP.put( id, this );
    }

    /**
     * Returns the MapleInternal enum object based on its name.
     * @param name of the internal maple object
     * @return MapleInternal object with the specified name.
     */
    public static MapleInternal getInternal( String name ){
        return Holder.INTERNAL_MAP.get(name);
    }

    @Override
    public String toString(){
        return id;
    }
}
