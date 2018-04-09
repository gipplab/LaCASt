package gov.nist.drmf.interpreter.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFLinker {

    private static final Logger LOG = LogManager.getLogger(DLMFLinker.class.getName());

    private final HashMap<String, String> link_librarie;

    public DLMFLinker( Path definitionFile ) {
        link_librarie = new HashMap<>();
        LOG.info("Start Link-Lib Init...");

        try ( BufferedReader br = Files.newBufferedReader(definitionFile) ){
            br.lines()
                    .parallel()
                    .map( l -> l.split("=>") )
                    .filter( l -> l.length == 2 )
                    .forEach( l -> {
                        String t = l[1].trim().substring("DLMF:/".length());
                        link_librarie.put( l[0].trim(), "http://dlmf.nist.gov/"+t );
                    } );
        } catch ( Exception e ){
            e.printStackTrace();
            return;
        }
    }

    public String getLink( String label ){
        return link_librarie.get(label);
    }
}
