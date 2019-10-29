package gov.nist.drmf.interpreter.cas.blueprints;

import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class BlueprintLimitNode {
    private static final Logger LOG = LogManager.getLogger(BlueprintLimitNode.class.getName());

    public static final String VAR_TOKEN = "var";
    public static final String LOWER_BOUND_TOKEN = "numL";
    public static final String UPPER_BOUND_TOKEN = "numU";

    private static final Pattern VAR_NUM_PATTERN = Pattern.compile(VAR_TOKEN+"(\\d+)");
    private static final Pattern LOWER_PATTERN = Pattern.compile(LOWER_BOUND_TOKEN+"(\\d+)");
    private static final Pattern UPPER_PATTERN = Pattern.compile(UPPER_BOUND_TOKEN+"(\\d+)");

    public static final String GREEK_TAG = "GREEK";

    private BlueprintLimitTree parentTree;
    private String latex;
    private String tag;

    private boolean isLeaf;
    private boolean isVariablePlaceHolder;
    private boolean isUpperBound;
    private boolean isLowerBound;

    private LinkedList<BlueprintLimitNode> children;

    private PomTaggedExpression pte;

    private String prefix = "";

    BlueprintLimitNode(){}

    public BlueprintLimitNode(
            BlueprintLimitTree parentTree,
            String latex,
            String tag,
            PomTaggedExpression pte
    ) {
        this.parentTree = parentTree;
        this.isLeaf = true;
        if ( latex.matches(VAR_TOKEN+"[N\\d]*") ) this.isVariablePlaceHolder = true;
        else if ( latex.matches(LOWER_BOUND_TOKEN+"\\d+") ) this.isLowerBound = true;
        else if ( latex.matches(UPPER_BOUND_TOKEN+"\\d+") ) this.isUpperBound = true;
        this.latex = latex;
        this.tag = tag;
        this.pte = pte;
    }

    public BlueprintLimitNode(
            BlueprintLimitTree parentTree,
            LinkedList<BlueprintLimitNode> children,
            PomTaggedExpression pte
    ) {
        this.parentTree = parentTree;
        this.isLeaf = false;
        this.children = children;
        this.pte = pte;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public PomTaggedExpression getMLPNode() {
        return pte;
    }

    public String getPrefix() {
        return prefix;
    }

    //    public boolean isVariablePlaceHolder() {
//        return isVariablePlaceHolder;
//    }
//
//    public boolean isUpperBound() {
//        return isUpperBound;
//    }
//
//    public boolean isLowerBound() {
//        return isLowerBound;
//    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof BlueprintLimitNode) ) {
            return false;
        }

        BlueprintLimitNode other = (BlueprintLimitNode) obj;

        // if the pattern is a sequence, the other element must be a sequence also
        if ( !isLeaf && other.isLeaf() ){
            return false;
        }

        if ( isLeaf ) {
            LOG.error("A pattern seems to be broken. Check it! " + parentTree);
            return false;
        }

        // so both are sequences: check the children
        return equalChildren(other);
    }

    private void addPrefexToLatex(String prefix){
        this.prefix = prefix;
    }

    private void setPTE(PomTaggedExpression pte) {
        this.pte = pte;
    }

    /**
     *
     * @param other
     * @return
     */
    private boolean equalChildren( BlueprintLimitNode other ) {
        if ( children.size() > other.children.size() ) {
            return false; // the pattern cannot be bigger than the actual limit
        }

        LinkedList<BlueprintLimitNode> patternCopy = new LinkedList<>(this.children);
        LinkedList<BlueprintLimitNode> refCopy = new LinkedList<>(other.children);

        while ( !patternCopy.isEmpty() ) {
            BlueprintLimitNode pattern = patternCopy.removeFirst();
            BlueprintLimitNode ref = refCopy.removeFirst();

            // first, if the pattern expects subexpression, go deeper!
            if ( !pattern.isLeaf ){
                // ok check if this subexpression matches, if NOT return false
                if ( !pattern.equals(ref) ) return false;
                // if it matches, move on till the end
            }

            // ok, pattern is a leaf... there are some options now
            // 1) its a variable place holder
            if ( pattern.isVariablePlaceHolder ) {
                // the reference MUST be one or more variables!
                if ( !possibleVar(ref) ) {
                    return false;
                }

                checkIfNextIsConnected(refCopy, ref);

                if ( pattern.latex.matches(VAR_TOKEN+"N") ) { // multi vars
                    parentTree.addVar(ref);
                    ref = refCopy.removeFirst();
                    while ( isListSplitter(ref) ) {
                        ref = refCopy.removeFirst();
                        if ( possibleVar(ref) ){
                            parentTree.addVar(ref);
                        } else {
                            return false;
                        }
                        checkIfNextIsConnected(refCopy, ref);
                        ref = refCopy.removeFirst();
                    }
                    // no more list splitters, so push the last element back
                    refCopy.addFirst(ref);
                } else {
                    Matcher m = VAR_NUM_PATTERN.matcher(pattern.latex);
                    if ( m.matches() ) {
                        int idx = Integer.parseInt(m.group(1));

                        parentTree.addVar(ref, idx);
                    }
                }
            } // 2) its an upper bound
            else if ( pattern.isUpperBound ) {
                Matcher m = UPPER_PATTERN.matcher(pattern.latex);
                if ( m.matches() ){
                    int idx = Integer.parseInt(m.group(1));

                    String prefix = "";
                    if ( ref.isLeaf && ref.latex.matches("-") ){
                        prefix = "- ";
                        ref = refCopy.removeFirst();
                        ref.addPrefexToLatex(prefix);
                    }
                    checkIfNextIsConnected(refCopy, ref);
                    parentTree.addUpperLimit(ref, idx);
                }
            } // 3) its a lower bound
            else if ( pattern.isLowerBound ) {
                Matcher m = LOWER_PATTERN.matcher(pattern.latex);
                if ( m.matches() ){
                    int idx = Integer.parseInt(m.group(1));

                    String prefix = "";
                    if ( ref.isLeaf && ref.latex.matches("-") ){
                        prefix = "- ";
                        ref = refCopy.removeFirst();
                        ref.addPrefexToLatex(prefix);
                    }
                    checkIfNextIsConnected(refCopy, ref);
                    parentTree.addLowerLimit(ref, idx);
                }
            }
            else { // must be another token, only exact matches are allowed!
                if ( ref.latex.matches("\\\\in") ) parentTree.overSet(true);
                if ( !pattern.latex.equals(ref.latex) ) return false;
            }
        }

        return refCopy.isEmpty();
    }

    private void checkIfNextIsConnected(LinkedList<BlueprintLimitNode> refCopy, BlueprintLimitNode ref) {
        if ( refCopy.isEmpty() ) return;
        if ( refCopy.get(0).tag.matches("underscore|caret") ) {
            PomTaggedExpression pte = new PomTaggedExpression(new MathTerm("", ""), "sequence");
            pte.addComponent(ref.pte);
            pte.addComponent(refCopy.removeFirst().pte);
            ref.setPTE(pte);
        }
    }

    private boolean possibleVar(BlueprintLimitNode node){
        return node.tag.equals("letter")
                || node.tag.equals("Latin")
                || node.tag.equals(GREEK_TAG)
                || node.tag.equals("alphanumeric");
    }

    private boolean isListSplitter(BlueprintLimitNode node){
        return node.tag.equals("comma");
    }
}
