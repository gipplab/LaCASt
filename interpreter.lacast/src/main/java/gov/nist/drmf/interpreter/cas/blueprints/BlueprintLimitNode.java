package gov.nist.drmf.interpreter.cas.blueprints;

import gov.nist.drmf.interpreter.cas.common.DLMFPatterns;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.mlp.extensions.FakeMLPGenerator;
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
    private boolean isLeafAndParent;

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
        this.isLeafAndParent = false;
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
        this.isLeafAndParent = false;
    }

    public BlueprintLimitNode(
            BlueprintLimitTree parentTree,
            LinkedList<BlueprintLimitNode> children,
            PomTaggedExpression pte,
            String latex,
            String tag
    ) {
        this(parentTree, latex, tag, pte);
        this.isLeaf = false;
        this.children = children;
        this.isLeafAndParent = true;
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

        // so both are sequences: check the children
        return equalChildren(other);
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
        LinkedList<BlueprintLimitNode> patternCopy, refCopy;

        if ( children == null || children.isEmpty() ) {
            if ( other.children != null && other.children.isEmpty() ) return false;
            patternCopy = new LinkedList<>();
            refCopy = new LinkedList<>();
            patternCopy.add(this);
            refCopy.add(other);
        } else {
            if ( children.size() > other.children.size() ) {
                return false; // the pattern cannot be bigger than the actual limit
            }
            patternCopy = new LinkedList<>(this.children);
            refCopy = new LinkedList<>(other.children);
        }

        while ( !patternCopy.isEmpty() ) {
            BlueprintLimitNode pattern = patternCopy.removeFirst();
            BlueprintLimitNode ref = refCopy.removeFirst();

            // first, if the pattern expects subexpression, go deeper!
            // ok check if this subexpression matches, if NOT return false
            if ( pattern.isLeafAndParent || !pattern.isLeaf) {
                if ( !pattern.equals(ref) ) return false;
                // if its true, we check if this pattern also matches
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
                    if ( refCopy.isEmpty() ) break;
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

                    PomTaggedExpression seq = FakeMLPGenerator.generateEmptySequencePTE();
                    PomTaggedExpression first = ref.pte;
                    seq.addComponent(first);

                    if (!addUntilEnd(patternCopy, refCopy, seq)) return false;

                    if ( seq.getComponents().size() > 1 ) ref.setPTE(seq);
                    parentTree.addUpperLimit(ref, idx);
                }
            } // 3) its a lower bound
            else if ( pattern.isLowerBound ) {
                Matcher m = LOWER_PATTERN.matcher(pattern.latex);
                if ( m.matches() ){
                    int idx = Integer.parseInt(m.group(1));

                    PomTaggedExpression seq = FakeMLPGenerator.generateEmptySequencePTE();
                    PomTaggedExpression first = ref.pte;
                    seq.addComponent(first);

                    if (!addUntilEnd(patternCopy, refCopy, seq)) return false;

                    if ( seq.getComponents().size() > 1 ) ref.setPTE(seq);
                    parentTree.addLowerLimit(ref, idx);
                }
            }
            else { // must be another token, only exact matches are allowed!
                if ( ref.latex.matches("\\\\in") )
                    parentTree.overSet(true);
                if ( !pattern.latex.equals(ref.latex) )
                    return false;
            }
        }

        return refCopy.isEmpty();
    }

    private boolean addUntilEnd(LinkedList<BlueprintLimitNode> patternCopy, LinkedList<BlueprintLimitNode> refCopy, PomTaggedExpression seq) {
        if ( patternCopy.isEmpty() ) {
            // just add all the rest
            while( !refCopy.isEmpty() )
                seq.addComponent(refCopy.removeFirst().pte);
        } else {
            BlueprintLimitNode nextPattern = patternCopy.get(0);
            BlueprintLimitNode nextRef = refCopy.remove(0);
            while( !nextRef.equals(nextPattern) ) {
                if ( nextRef.latex != null && nextRef.latex.matches(
                        "\\\\[ci]dot.*|\\\\(?:ne|ge|le)q?|[<>=].*") ){
                    break;
                }

                seq.addComponent(nextRef.pte);
                if ( refCopy.isEmpty() ) return false;
                nextRef = refCopy.remove(0);
            }
            // if it reaches this point, the last two equaled or we reached a stop signal
            refCopy.addFirst(nextRef);
        }
        return true;
    }

    private void checkIfNextIsConnected(LinkedList<BlueprintLimitNode> refCopy, BlueprintLimitNode ref) {
        if ( refCopy.isEmpty() ) return;
        if ( refCopy.get(0).tag.matches(
                MathTermTags.underscore.tag() + "|" + MathTermTags.caret.tag()
        ) ) {
            PomTaggedExpression pte = FakeMLPGenerator.generateEmptySequencePTE();
            pte.addComponent(ref.pte);
            pte.addComponent(refCopy.removeFirst().pte);
            ref.setPTE(pte);
        }
    }

    private boolean possibleVar(BlueprintLimitNode node){
        if ( node == null || node.tag == null ) return false;
        return node.tag.equals(MathTermTags.letter.tag())
                || node.tag.equals("Latin")
                || node.tag.equals(MathTermTags.special_math_letter.tag())
                || node.tag.equals(GREEK_TAG)
                || node.tag.equals(MathTermTags.alphanumeric.tag());
    }

    private boolean isListSplitter(BlueprintLimitNode node){
        return node.tag.equals(MathTermTags.comma.tag());
    }
}
