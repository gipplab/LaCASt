package gov.nist.drmf.interpreter.generic.mlp.struct;

import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.formulasearchengine.mathosphere.mlp.pojos.Position;
import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
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
            Position p1;
            if ( formula.getPositions().isEmpty() )
                p1 = new Position(0,0,0);
            else p1 = formula.getPositions().get(0);

            Position p2;
            if ( o.formula.getPositions().isEmpty() )
                p2 = new Position(0,0,0);
            else p2 = o.formula.getPositions().get(0);

            return Position.getComparator().compare(p1, p2);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            // if something went wrong, we simply cannot compare it. So they are equal
            return 0;
        }
    }
}
