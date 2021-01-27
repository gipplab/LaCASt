package gov.nist.drmf.interpreter.common.latex;

import gov.nist.drmf.interpreter.common.meta.ListExtender;

import java.util.LinkedList;
import java.util.Objects;

import static java.util.function.Predicate.not;

/**
 * @author Andre Greiner-Petter
 */
public class RelationalComponents {

    private LinkedList<String> parts;
    private LinkedList<Relations> relations;

    public RelationalComponents() {
        parts = new LinkedList<>();
        relations = new LinkedList<>();
    }

    public RelationalComponents(RelationalComponents relationalComponents) {
        parts = new LinkedList<>(relationalComponents.parts);
        relations = new LinkedList<>(relationalComponents.relations);
    }

    public void addRelationalComponents(RelationalComponents relationalComponents) {
        ListExtender.addAll( parts, relationalComponents.parts, not(String::isBlank) );
        relations.addAll( relationalComponents.relations );
    }

    public void clear() {
        parts.clear();
        relations.clear();
    }

    public void addComponent( String component ) {
        parts.addLast(component);
    }

    public void addRelation( String symbol ) {
        addRelation( Relations.getRelation(symbol) );
    }

    public void addRelation( Relations rel ) {
        relations.addLast(rel);
    }

    public LinkedList<String> getComponents() {
        return parts;
    }

    public LinkedList<Relations> getRelations() {
        return relations;
    }
}
