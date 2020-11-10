package gov.nist.drmf.interpreter.generic.macro;

import gov.nist.drmf.interpreter.common.text.TextUtility;
import org.apache.logging.log4j.core.util.Integers;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java class for parsing a .sty file
 */
public class MacroDefinitionStyleFileParser {
    private static final Pattern FUNC_SPEC_PATTERN = Pattern.compile(
            "^\\\\defSpecFun\\{(.*?)}" + // func name (non-optional)
                    "(?:\\[(\\d+)])?(\\[])?" + // number of parameters (optional -> means 0)
                    "\\{(.*?)}\\[\\s*(?:%.*)?$" // definition
            , Pattern.MULTILINE
    );

    private static final Pattern IFX_PATTERN = Pattern.compile(
            "\\\\ifx?\\.#1\\.(.*?)\\\\else(.*?)\\\\fi"
    );

    private static final Pattern META_PATTERN = Pattern.compile(
            "(description|meaning|om|params|args)=(\\{.*?}|[^{].*?),"
    );

    private static final Pattern ARG_LIST_PATTERN = Pattern.compile("\\s*]\\{(\\d+)}(\\[.*])?$");

    private static final Pattern NUMBER_PATTERN = Pattern.compile("#(\\d+)");

    private final Map<String, MacroBean> macros;

    public MacroDefinitionStyleFileParser() {
        macros = new HashMap<>();
    }

    public void load(String input) {
        Matcher m = FUNC_SPEC_PATTERN.matcher(input);

        MacroBean currentBean = null;
        // first line
        if ( m.find() ) {
            currentBean = handleNewMacroHit(m);
        }

        while( m.find() ) {
            handleMetaDataAfterHit(currentBean, m, false);
            currentBean = handleNewMacroHit(m);
        }

        handleMetaDataAfterHit(currentBean, m, true);
    }

    private MacroBean handleNewMacroHit( Matcher m ) {
        String beanName = m.group(1);
        if ( beanName.matches("\\w+\\[]") ) {
            // is an optional macro (eg \FerrersP[]
            handleOptionalMacro(beanName, m);
            return null;
        } else {
            // is an normal new macro
            return handleActuallyNewMacro(beanName, m);
        }
    }

    /**
     * Alright, this means, the previous added (if not isIfxAdded()) was actually wrong
     * @param beanName
     * @param m
     */
    private void handleOptionalMacro(String beanName, Matcher m) {
        beanName = beanName.substring(0, beanName.length()-2);
        if ( macros.containsKey(beanName) ) {
            MacroBean currentBean = macros.get(beanName);

            if ( currentBean.isIfxAdded() ) {
                // in this case, the macro already defined an optional argument via \ifx.\else.\fi and we should end here...
                return;
            }

            if ( Objects.isNull(m.group(4)) )
                throw new IllegalArgumentException("Generic LaTeX is mandatory but was null for " + beanName);

            currentBean.flipLastToOptionalParameter();
            String genericLaTeX = cleanIfx(m.group(4)).get(0);
            int numOfParameter = Objects.nonNull(m.group(2)) ? Integers.parseInt(m.group(2)) : 0;
            currentBean.setGenericLaTeXParametersWithoutOptionalParameter(numOfParameter, genericLaTeX);
        } else {
            throw new IllegalArgumentException(
                    "Found optional argument macro before analyzing the standard macro: " + beanName
            );
        }
    }

    private MacroBean handleActuallyNewMacro(String beanName, Matcher m) {
        MacroBean currentBean = new MacroBean(beanName);
        int numOfParameter = Objects.nonNull(m.group(2)) ? Integers.parseInt(m.group(2)) : 0;
        if ( Objects.isNull(m.group(4)) )
            throw new IllegalArgumentException("Generic LaTeX is mandatory but was null for " + beanName);
        List<String> genericLaTeX = cleanIfx(m.group(4));

        if ( genericLaTeX.size() == 2 ) {
            String genericLaTeXOpt;
            // that means the first #1 argument is actually an optional argument
            currentBean.setGenericLaTeXParametersWithoutOptionalParameter(numOfParameter, reduceNumbers(genericLaTeX.get(0)));
            genericLaTeXOpt = genericLaTeX.get(1);
            currentBean.setIfxAdded(true);

            genericLaTeXOpt = genericLaTeXOpt.replaceAll("#1", MacroHelper.OPTIONAL_PAR_PREFIX+"1");
            genericLaTeXOpt = reduceNumbers(genericLaTeXOpt);
            currentBean.setGenericLaTeXParametersWithOptionalParameter(numOfParameter, genericLaTeXOpt);
        } else {
            currentBean.setGenericLaTeXParametersWithoutOptionalParameter(numOfParameter, genericLaTeX.get(0));
        }

        return currentBean;
    }

    private String reduceNumbers(String in) {
        StringBuilder sb = new StringBuilder();
        Matcher m = NUMBER_PATTERN.matcher(in);
        while ( m.find() ) {
            int n = Integers.parseInt(m.group(1));
            String newN = Integer.toString(n-1);
            m.appendReplacement(sb, MacroHelper.PAR_PREFIX + newN);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void handleMetaDataAfterHit( MacroBean currentBean, Matcher m, boolean last ) {
        // add meta info to last macro
        StringBuffer sb = new StringBuffer();
        if ( last ) m.appendTail(sb);
        else m.appendReplacement(sb, "");

        if ( Objects.nonNull(currentBean) ) {
            loadAdditionalInformation(currentBean, sb.toString());
            macros.put(Objects.requireNonNull(currentBean).getName(), currentBean);
        }
    }

    private List<String> cleanIfx(String in) {
        List<String> ifxList = new LinkedList<>();

        StringBuilder prefix = new StringBuilder();
        StringBuilder postfix = new StringBuilder();
        Matcher m = IFX_PATTERN.matcher(in);
        if ( m.find() ) {
            m.appendReplacement(prefix, "");
            m.appendTail(postfix);
            String withoutOpt = m.group(1);
            String withOpt = m.group(2);

            ifxList.add( prefix.toString() + withoutOpt + postfix.toString() );
            ifxList.add( prefix.toString() + withOpt + postfix.toString() );
        } else {
            ifxList.add(in);
        }

        return ifxList;
    }

    public Map<String, MacroBean> getExtractedMacros() {
        return macros;
    }

    private static void loadAdditionalInformation(MacroBean bean, String metaInfoString) {
        MacroMetaBean metaBean = loadMeta(metaInfoString);
        bean.setMetaInformation(metaBean);

        Matcher argumentMatcher = ARG_LIST_PATTERN.matcher(metaInfoString);
        if ( argumentMatcher.find() ) {
            bean.setGenericLaTeXArguments(
                    Integer.parseInt(argumentMatcher.group(1)),
                    argumentMatcher.group(2)
            );
        }
    }

    private static MacroMetaBean loadMeta(String metaInfoString) {
        Matcher metaMatcher = META_PATTERN.matcher(metaInfoString);
        MacroMetaBean metaBean = new MacroMetaBean();
        MacroStandardArgumentsBean standardArgBean = metaBean.getStandardArguments();

        while ( metaMatcher.find() ) {
            String key = metaMatcher.group(1);
            String value = metaMatcher.group(2);
            switch (key) {
                case "meaning":
                    metaBean.setMeaning(value);
                    break;
                case "om":
                    metaBean.setOpenMathID(value);
                    break;
                case "description":
                    metaBean.setDescription(value.substring(1, value.length()-1));
                    break;
                case "params":
                    standardArgBean.setStandardParameters(value.substring(1, value.length()-1));
                    break;
                case "args":
                    standardArgBean.setStandardVariables(value.substring(1, value.length()-1));
                    break;
            }
        }

        return metaBean;
    }
}
