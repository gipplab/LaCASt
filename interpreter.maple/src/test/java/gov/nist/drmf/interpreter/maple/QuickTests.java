package gov.nist.drmf.interpreter.maple;

import gov.nist.drmf.interpreter.common.GlobalPaths;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by AndreG-P on 28.02.2017.
 */
public class QuickTests {
    @Disabled
    @Test
    public void arraysTest(){
        String[] empty = new String[]{};
        assertEquals("[]", Arrays.toString(empty), "Not equal string!" );
        System.out.println("blabla:".split(":").length);
    }

    @Disabled
    @Test
    public void streamTest(){
        //Path p = Paths.get("interpreter.maple", "src", "test", "resources", "Test.txt");

        Path p = GlobalPaths.PATH_REFERENCE_DATA.resolve("Test.txt");
        try (BufferedReader reader = Files.newBufferedReader( p )){
            reader.lines()
                    .forEach( l -> {
                        try { innerReaderTest(reader, 3, l); }
                        catch( IOException ioe ){
                            System.err.println("Error in line " + l);
                        }
                    } );
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }

    private void innerReaderTest( BufferedReader reader, int length, String line )
            throws IOException{
        System.out.println(line);
        reader.readLine();
        reader.readLine();
    }
}
