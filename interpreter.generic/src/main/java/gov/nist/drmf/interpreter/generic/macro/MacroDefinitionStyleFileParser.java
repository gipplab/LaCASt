package gov.nist.drmf.interpreter.generic.macro;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java class for parsing a .sty file
 */
public class MacroDefinitionStyleFileParser {
    private static final Pattern FUNC_SPEC_PATTERN = Pattern.compile(
            "^\\\\defSpecFun\\{(.*?)}" + // func name (non-optional)
                    "(\\[\\d+])?(?:\\[])?" + // number of parameters (optional -> means 0)
                    "\\{(.*?)}\\[.*$" // definition
            , Pattern.MULTILINE
    );

    private static final Pattern META_PATTERN = Pattern.compile(
            "(description|meaning|om|params|args)=\\{?(.*?)}?,"
    );

    private static final Pattern ARG_LIST_PATTERN = Pattern.compile("\\s*]\\{(\\d+)}(\\[.*])?$");

    private LinkedList<MacroBean> macros;

    public MacroDefinitionStyleFileParser() {
        macros = new LinkedList<>();
    }

    public void load(String input) {
        StringBuffer sb;
        Matcher m = FUNC_SPEC_PATTERN.matcher(input);

        MacroBean currentBean = null;
        // first line
        if ( m.find() ) {
            String name = m.group(1);
            currentBean = new MacroBean(name);
            currentBean.setGenericLaTeXParameters(m.group(3));
        }

        while( m.find() ) {
            // fill up the rest info from the previous hit
            sb = new StringBuffer();
            m.appendReplacement(sb, "");
            loadMeta(currentBean, sb.toString());
            macros.add(currentBean);

            // done, get next
            currentBean = new MacroBean(m.group(1));
            currentBean.setGenericLaTeXParameters(m.group(3));
        }

        // add meta info to last macro
        sb = new StringBuffer();
        m.appendTail(sb);
        loadMeta(currentBean, sb.toString());
        macros.add(currentBean);
    }

    public List<MacroBean> getListOfExtractedMacros() {
        return macros;
    }

    private static void loadMeta(MacroBean bean, String metaInfoString) {
        Matcher metaMatcher = META_PATTERN.matcher(metaInfoString);
        while ( metaMatcher.find() ) {
            String key = metaMatcher.group(1);
            String value = metaMatcher.group(2);
            switch (key) {
                case "meaning":
                    bean.setMeaning(value);
                    break;
                case "om":
                    bean.setOpenMathID(value);
                    break;
                case "description":
                    bean.setDescription(value);
                    break;
                case "params":
                    bean.setStandardParameters(value);
                    break;
                case "args":
                    bean.setStandardArguments(value);
                    break;
            }
        }

        Matcher argumentMatcher = ARG_LIST_PATTERN.matcher(metaInfoString);
        if ( argumentMatcher.find() ) {
            bean.setGenericLaTeXArguments(
                    Integer.parseInt(argumentMatcher.group(1)),
                    argumentMatcher.group(2)
            );
        }
    }
}
