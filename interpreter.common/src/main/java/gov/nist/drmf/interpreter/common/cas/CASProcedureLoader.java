package gov.nist.drmf.interpreter.common.cas;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public class CASProcedureLoader {
    private static final Logger LOG = LogManager.getLogger(CASProcedureLoader.class.getName());

    public static String getProcedure(Path path) {
        try (Stream<String> stream = Files.lines(path)){
            String procedures = stream.collect( Collectors.joining(System.lineSeparator()) );
            stream.close();
            LOG.debug("Successfully loaded procedures from " + path);
            return procedures;
        } catch (IOException ioe){
            LOG.error("Cannot load procedure from file " + path, ioe);
            return null;
        }
    }
}
