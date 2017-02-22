package gov.nist.drmf.interpreter.maple.grammar;

/**
 *
 * @see <a href="http://www.maplesoft.com/support/help/Maple/view.aspx?path=ProgrammingGuide/Appendix1">Maple Internal Representation</a>
 * Created by AndreG-P on 21.02.2017.
 */
public enum MapleInternal {
    /**
     * SUM and EXPSEQ
     */
    sum("SUM"),             // [SUM, summand_1, summand_2, ...]
    exp("EXPSEQ"),          // [EXPSEQ, exp_1, exp_2, ...], usually represented a list, set or function calls

    /**
     * Numerical expressions
     */
    intpos("INTPOS"),       // [INTPOS, integer]
    intneg("INTNEG"),       // [INTNEG, integer]
    complex("COMPLEX"),     // [COMPLEX, RE, IM] OR [COMPLEX, IM]
    floating("FLOAT"),      // [FLOAT, integer_1, integer_2], means integer_1*10^integer_2
    rational("RATIONAL"),   // [RATIONAL, integer, pos_integer], 5/(-2) is [RATIONAL, -5, 2]

    prod("PROD"),           // [PROD, exp1, exp1_exponent, exp2, exp2_exponent, ...]
    power("POWER"),         // [POWER, exp1, exp2] exp1^exp2
    function("FUNCTION"),   // [FUNCTION, [ASSIGNEDNAME, "func_name", ...], [EXPSEQ, ...]]

    // [ASSIGNEDNAME, func_name as String, "PROC", [ATTRIBUTE, ...]]
    // sometimes also [ASSIGNEDNAME, name_of_variable, domains], [ASSIGNEDNAME, "a", "INTPOS"]
    ass_name("ASSIGNEDNAME"),
    name("NAME"),           // [NAME, variable or something else as String]
    string("STRING"),       // [STRING, "string"]

    equation("EQUATION"),   // [EQUATION, exp1, exp2] exp1 = exp2
    ineq("INEQUAT"),        // [INEQAT, exp1, exp2] <>
    lesseq("LESSEQ"),       // [LESSEQ, exp1, exp2] >= or <=
    lessthan("LESSTHAN"),   // [LESSTHAN, exp1, exp2] > or <
    imply("IMPLIES"),       // [IMPLIES, exp1, exp2] =>

    not("NOT"),             // [NOT, exp] logical not
    or("OR"),               // [OR, exp1, exp2] exp1 OR exp2
    xor("XOR"),             // [XOR, exp1, exp2] exp1 XOR exp2

    set("SET"),             // [SET, exp_seq, attributes] {exp1, exp2, ...}

    // TODO problematics:
    //list("LIST"),           // [LIST, [EXPSEQ, ...]]
    //poly("POLY"),           // ...
    //range("RANGE"),         // [RANGE, exp1, exp2] exp1 .. exp2
    //statseq("STATSEQ"),     // [STATSEQ, stat1, stat2, ...] stat1; stat2; ...
    ;

    private String id;

    private MapleInternal( String id ){
        this.id = id;
    }
}
