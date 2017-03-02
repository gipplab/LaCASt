package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.maple.grammar.TranslatedExpression;

import java.util.LinkedList;

/**
 * Created by AndreG-P on 02.03.2017.
 */
public class TranslationFailures {

    private static final String new_line = System.lineSeparator();
    private LinkedList<Failure> failures;

    public TranslationFailures(){
        this.failures = new LinkedList<>();
    }

    public void addFailure( String message ){
        Failure f = new Failure();
        f.message = message;
        failures.add( f );
    }

    public void addFailure( String message, Class location ){
        addFailure(message);
        failures.getLast().location = location;
    }

    public void addFailure( String message, Class location, String expression ){
        addFailure( message, location );
        failures.getLast().expression = expression;
    }

    public boolean isEmpty(){
        return failures.isEmpty();
    }

    public String getFailures(){
        String output = "";
        for ( int i = 0; i < failures.size(); i++ )
            output += failures.get(i) + new_line;
        return output;
    }

    private class Failure {
        String message, expression;
        Class location;

        @Override
        public String toString(){
            String output = "";
            if ( location != null )
                output = location.getName() + ": ";
            if ( expression != null )
                return output + message + " <-> " + expression;
            else return output + message;
        }
    }
}
