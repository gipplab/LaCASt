package gov.nist.drmf.interpreter.constraints;

import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.evaluation.NumericalEvaluator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * @author Andre Greiner-Petter
 */
public class MLPBlueprintNode {

    private static final Logger LOG = LogManager.getLogger(MLPBlueprintNode.class.getName());

    public static final String GREEK = "GREEK";

    private String latex;

    private String tag;

    private ArrayList<MLPBlueprintNode> children;

    private MLPBlueprintTree tree;

    private MapleTranslator translator;

    public MLPBlueprintNode(MLPBlueprintTree tree, String latex, String tag) {
        this.tree = tree;
        this.latex = latex;
        this.tag = tag;
        this.translator = NumericalEvaluator.getTranslator();
    }

    public MLPBlueprintNode(MLPBlueprintTree tree, ArrayList<MLPBlueprintNode> children) {
        this.tree = tree;
        this.children = children;
        this.translator = NumericalEvaluator.getTranslator();
    }

    public boolean isLeaf() {
        return children == null;
    }

    public String getLatex() {
        return latex;
    }

    public String getTag() {
        return tag;
    }

    public ArrayList<MLPBlueprintNode> getChildren() {
        return children;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MLPBlueprintNode)) {
            return false;
        }

        MLPBlueprintNode other = (MLPBlueprintNode) obj;

        // if one is leaf but the other is not, return true
        if (isLeaf() != other.isLeaf()) {
            LOG.trace("Leaf-vs-NonLeaf check returns false.");
            return false;
        }

        // now we know, both are leafs or both are non leafs.
        // in case of non-leaf -> investigate children
        if (!isLeaf()) {
            return equalChildren(other);
        }

        LOG.trace(String.format("Check leafs: %s vs %s", latex, other.latex));

        // otherwise its a leaf
        // first, check if its a VAR.
        if (latex.matches(MLPBlueprintTree.VAR)) {
            if (possibleVar(other)) {
                String mapleVar = translator.translateFromLaTeXToMapleClean(other.latex);
                tree.setVariable(latex, mapleVar);
                LOG.trace(String.format("Found variable match. Set %s = %s.",latex, mapleVar));
                return true;
            }
            return false;
        }

        // equal function should be commutative, so try the other way around
        if (other.latex.matches(MLPBlueprintTree.VAR)) {
            if (possibleVar(this)) {
                String mapleVar = translator.translateFromLaTeXToMapleClean(latex);
                other.tree.setVariable(other.latex, mapleVar);
                LOG.trace(String.format("Found variable match. Set %s = %s.",other.latex, mapleVar));
                return true;
            }
            return false;
        }

        // now check tags
        if (!tag.equals(other.tag)) {
            LOG.trace(String.format("Differences in tags were detected: %s vs %s.", tag, other.tag));
            return false;
        }

        // last step, check actual string
        return latex.equals(other.latex);
    }

    private boolean possibleVar(MLPBlueprintNode node) {
        return node.tag.equals("letter")
                || node.tag.equals("Latin")
                || node.tag.equals(GREEK)
                || node.tag.equals("alphanumeric");
    }

    private boolean equalChildren(MLPBlueprintNode other) {
        ArrayList<MLPBlueprintNode> otherChildren = other.children;

        // different length
        if (children.size() != otherChildren.size()) {
            return false;
        }

        LOG.trace(String.format("Check %d children.", children.size()));

        for (int i = 0; i < children.size(); i++) {
            if (!children.get(i).equals(otherChildren.get(i))) {
                return false;
            }
        }

        return true;
    }
}
