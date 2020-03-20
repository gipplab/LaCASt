package gov.nist.drmf.interpreter.evaluation.core.diff;

import com.wolfram.jlink.Expr;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.interfaces.ITranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class SingleNumericEntity {
    private static final Logger LOG = LogManager.getLogger(SingleNumericEntity.class.getName());

    public static HashMap<String, String> translationCache = new HashMap<>();
    public static final Pattern SUBSCRIPT_PATTERN = Pattern.compile("^(.*)\\[(.*)]$");

    private String value;
    private String variables;

    private boolean isMathematica;

    public SingleNumericEntity(String value, String variables, boolean isMathematica) {
        this.value = value;
        this.variables = variables;
        this.isMathematica = isMathematica;
        if ( !isMathematica ) {
            this.value = value.replaceAll("e", "*^");
        }
    }

    public boolean match(SingleNumericEntity sne, ITranslator forwardTranslator, ITranslator backwardTranslator, IComputerAlgebraSystemEngine<Expr> engine) throws ComputerAlgebraSystemEngineException {
        if (sne.isMathematica) return false;

        String[] vars = sne.variables.split(", ");

        StringBuilder varBuilder = new StringBuilder();
        StringBuilder sb = new StringBuilder("{");
        for ( int i = 0; i < vars.length; i++ ) {
            String v = vars[i];
            if ( v.isEmpty() ) {
                return variables.isEmpty();
            }

            String[] lr = v.split(" = ");

            String leftRef = translationCache.get(lr[0]);
            if ( leftRef == null ) {
                Matcher m = SUBSCRIPT_PATTERN.matcher(lr[0]);
                if ( m.matches() ) {
                    leftRef = "Subscript[" + m.group(1) + ", " + m.group(2) + "]";
                } else {
                    try {
                        leftRef = backwardTranslator.translate(lr[0]);
                    } catch ( TranslationException te ) {
                        LOG.error("Cannot backward translate " + lr[0], te);
                        throw te;
                    }
                    try {
                        leftRef = forwardTranslator.translate(leftRef);
                    } catch ( TranslationException te ) {
                        LOG.error("Cannot forward translate " + leftRef, te);
                        throw te;
                    }
                }
                translationCache.put( lr[0], leftRef );
            }

            varBuilder.append(leftRef);
            sb.append(leftRef).append(" == ").append(lr[1]);
            if ( i < vars.length-1 ) {
                varBuilder.append(", ");
                sb.append(", ");
            }
        }
        sb.append("}");

        Expr res = engine.enterCommand("ClearAll[" + varBuilder.toString() + "]");
//        LOG.debug("Reset variables: " + res.toString());

        String command = "Select[ReplaceAll["+sb.toString()+", {"+variables+"}], Not[#] &]";
        res = engine.enterCommand(command);
        return res.toString().matches("\\{}");
    }

    public static String buildMathematicaValuesList(LinkedList<SingleNumericEntity> list) {
        return buildMathematicaValuesList(list, true);
    }

    public static String buildMathematicaValuesList(LinkedList<SingleNumericEntity> list, boolean value) {
        StringBuilder sb = new StringBuilder("{");
        if (value) sb.append(list.get(0).value);
        else {
            sb.append("\"");
            sb.append(list.get(0).variables);
            sb.append("\"");
        }

        for ( int i = 1; i < list.size(); i++ ) {
            SingleNumericEntity e = list.get(i);
            sb.append(", ");
            if ( value ) sb.append(e.value);
            else {
                sb.append("\"");
                sb.append(e.variables);
                sb.append("\"");
            }
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "[" + value + " <- {" + variables + "}]";
    }
}
