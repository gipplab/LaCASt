package gov.nist.drmf.interpreter.examples;

import mlp.PomParser;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public class ExampleParser {

    private String last_equation = "";

    private String maple = "";
    private String link_dlmf = "";
    private String link_maple = "";
    private List<String> constraints;

    private PomParser parser;

    public ExampleParser(){
        constraints = new LinkedList<String>();

    }

    public void parse(String equation){
        // TODO
    }

    public String getMapleRepresentation(){
        return null;
    }

    public List<String> getConstraints(){
        return null;
    }

    public String getDLMFDefinition(){
        return null;
    }

    public String getMapleDefinition(){
        return null;
    }
}
