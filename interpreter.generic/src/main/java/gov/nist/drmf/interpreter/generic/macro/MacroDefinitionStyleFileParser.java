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
            "^\\s*\\\\defSpecFun.*$"
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

    private static final Pattern COMMENT_LINE = Pattern.compile("^\\s*%.*\n?$",
            Pattern.MULTILINE);

    private static final Pattern OPT_ARG_SPEC_PATTERN = Pattern.compile("\\[\\d+]\\[]");

    private final Map<String, MacroBean> macros;

    public MacroDefinitionStyleFileParser() {
        macros = new HashMap<>();
    }

    private String deleteCommentLines(String input) {
        StringBuilder sb = new StringBuilder();
        Matcher m = COMMENT_LINE.matcher(input);
        while(m.find()) {
            m.appendReplacement(sb, "");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public void load(String input) {
        input = deleteCommentLines(input);
        Matcher m = FUNC_SPEC_PATTERN.matcher(input);

        MacroBean currentBean = null;
        // first line
        if ( m.find() ) {
            currentBean = handleNewMacroHit(m.group(0));
        }

        while( m.find() ) {
            handleMetaDataAfterHit(currentBean, m, false);
            currentBean = handleNewMacroHit(m.group(0));
        }

        handleMetaDataAfterHit(currentBean, m, true);
        loadWignerSymbols();
    }

    private void loadWignerSymbols() {
        MacroWignerSymbols wignerSymbols = new MacroWignerSymbols();
        for ( MacroWignerSymbols.Type type : MacroWignerSymbols.Type.values() ) {
            macros.put( type.getMacro(), wignerSymbols.getWignerSymbol(type) );
        }
    }

    private MacroBean handleNewMacroHit( String line ) {
        LinkedList<String> args = new LinkedList<>();
        String numberOfParameterStr = parseDefSpecLine(line, args);

        String macroName = args.get(0);
        if ( macroName.contains("[") ) {
            handleOptionalMacro(args, numberOfParameterStr);
            return null;
        } else {
            return handleMacro(args, numberOfParameterStr, line);
        }
    }

    private MacroBean handleMacro(LinkedList<String> args, String numberOfParameterStr, String defLine) {
        MacroBean currentBean = new MacroBean(args.removeFirst());
        int numOfParameter = Objects.nonNull(numberOfParameterStr) ? Integers.parseInt(numberOfParameterStr) : 0;
        if ( args.isEmpty() )
            throw new IllegalArgumentException("Generic LaTeX is mandatory but was null for " + currentBean.getName());
        LinkedList<String> genericLaTeX = cleanIfx(args.removeFirst());

        if ( genericLaTeX.size() == 2 && hasOptionalArgument(defLine) ) {
            String genericLaTeXOpt;
            // that means the first #1 argument is actually an optional argument
            currentBean.setGenericLaTeXParametersWithoutOptionalParameter(numOfParameter, reduceNumbers(genericLaTeX.get(0)));
            genericLaTeXOpt = genericLaTeX.get(1);
            currentBean.setIfxAdded(true);

            genericLaTeXOpt = genericLaTeXOpt.replaceAll("#1", MacroHelper.OPTIONAL_PAR_PREFIX+"1");
            genericLaTeXOpt = reduceNumbers(genericLaTeXOpt);
            currentBean.setGenericLaTeXParametersWithOptionalParameter(numOfParameter, genericLaTeXOpt);
        } else {
            currentBean.setGenericLaTeXParametersWithoutOptionalParameter(numOfParameter, genericLaTeX.getLast());
        }

        return currentBean;
    }

    private void handleOptionalMacro(LinkedList<String> args, String numOfParamsString) {
        String beanName = args.removeFirst();
        beanName = beanName.substring(0, beanName.length()-2);
        if ( macros.containsKey(beanName) ) {
            MacroBean currentBean = macros.get(beanName);

            if ( currentBean.isIfxAdded() ) {
                // in this case, the macro already defined an optional argument via \ifx.\else.\fi and we should end here...
                return;
            }

            if ( args.isEmpty() )
                throw new IllegalArgumentException("Generic LaTeX is mandatory but was null for " + beanName);

            currentBean.flipLastToOptionalParameter();
            String genericLaTeX = cleanIfx(args.removeFirst()).get(0);
            int numOfParameter = Objects.nonNull(numOfParamsString) ? Integers.parseInt(numOfParamsString) : 0;
            currentBean.setGenericLaTeXParametersWithoutOptionalParameter(numOfParameter, genericLaTeX);
        } else {
            throw new IllegalArgumentException(
                    "Found optional argument macro before analyzing the standard macro: " + beanName
            );
        }
    }

    private boolean hasOptionalArgument(String line) {
        Matcher m = OPT_ARG_SPEC_PATTERN.matcher(line);
        return m.find();
    }

    private String parseDefSpecLine(String line, LinkedList<String> args) {
        String numberOfParameterStr = null;
        int open = 0;
        int startIdx = 11;
        for ( int i = 11; i < line.length(); i++ ) {
            char c = line.charAt(i);
            if ( open == 0 && '[' == c && args.size() < 2 ) {
                startIdx = i+1;
                i = untilClosedBracket(line, i);
                if ( i > startIdx ) {
                    numberOfParameterStr = line.substring(startIdx, i);
                }
            } else if ( '{' == c ) {
                if ( open == 0 ) startIdx = i+1;
                open++;
            } else if ( '}' == c ) {
                open--;
                if ( open == 0 ) {
                    String arg = line.substring(startIdx, i);
                    args.addLast(arg);
                }
            } else if ( '%' == c ) break;
        }
        return numberOfParameterStr;
    }

    private int untilClosedBracket(String line, int i) {
        char c = line.charAt(i);
        while ( c != ']' && i+1 < line.length() ) {
            i = i+1;
            c = line.charAt(i);
        }
        return i;
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

    private LinkedList<String> cleanIfx(String in) {
        LinkedList<String> ifxList = new LinkedList<>();

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
        loadMeta(bean, metaInfoString);

        Matcher argumentMatcher = ARG_LIST_PATTERN.matcher(metaInfoString);
        if ( argumentMatcher.find() ) {
            bean.setGenericLaTeXArguments(
                    Integer.parseInt(argumentMatcher.group(1)),
                    argumentMatcher.group(2)
            );
        }
    }

    private static void loadMeta(MacroBean bean, String metaInfoString) {
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
                    standardArgBean.setStandardParameters(bean.getNumberOfOptionalParameters(), value.substring(1, value.length()-1));
                    break;
                case "args":
                    standardArgBean.setStandardVariables(value.substring(1, value.length()-1));
                    break;
            }
        }

        bean.setMetaInformation(metaBean);
    }
}
