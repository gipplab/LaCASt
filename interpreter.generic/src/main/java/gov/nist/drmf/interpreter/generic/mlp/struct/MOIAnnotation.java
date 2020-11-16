package gov.nist.drmf.interpreter.generic.mlp.struct;

import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class MOIAnnotation implements Comparable<MOIAnnotation> {
    private final String id;
    private MathTag formula;
    private List<Relation> attachedRelations;

    public MOIAnnotation() {
        this.id = "-1";
        this.formula = null;
        this.attachedRelations = new LinkedList<>();
    }

    public MOIAnnotation(MathTag mathTag) {
        this.id = mathTag.placeholder();
        this.formula = mathTag;
        this.attachedRelations = new LinkedList<>();
    }

    public void appendRelation(Relation relation) {
        this.attachedRelations.add(relation);
    }

    public MathTag getFormula() {
        return formula;
    }

    public List<Relation> getAttachedRelations() {
        return attachedRelations;
    }

    @Override
    public int compareTo(@NotNull MOIAnnotation o) {
        try {
            return formula.getPositions().get(0).compareTo(o.formula.getPositions().get(0));
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            // if something went wrong, we simply cannot compare it. So they are equal
            return 0;
        }
    }
}
