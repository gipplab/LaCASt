package gov.nist.drmf.interpreter.evaluation;

import java.util.Arrays;

/**
 * @author Andre Greiner-Petter
 */
public enum Status {
    SUCCESS(0),
    SUCCESS_SYMB(0),
    SUCCESS_TRANS(0),
    FAILURE(0),
    SKIPPED(0),
    ERROR(0);

    private int counter;

    Status( int counter ){
        this.counter = counter;
    }

    public static void reset(){
        for ( Status s : Status.values() )
            s.counter = 0;
    }

    public void add(){
        this.counter++;
    }

    public void set(int counter){
        this.counter = counter;
    }

    @Override
    public String toString(){
        return this.name() + ": " + counter;
    }

    public static String buildString(){
        return Arrays.toString(Status.values());
    }
}
